# Desktop memory tuning

Why the desktop app's RAM use was cut, what each change does, and how to undo any of it.

Investigated 2026-07-29 against `45260568` (dev). Every number below was measured on a live
nightly AppImage, not estimated.

---

## The problem

The desktop app settled at roughly **1 GB resident on all three platforms**, and on Linux it kept
climbing the longer a listening session ran — reaching **1.90 GB after 1h40m**.

This had been reported across Windows, macOS and Linux, which was the clue that mattered: a bug
confined to one platform's code would not show up identically on three different OSes.

## What it was NOT

Two plausible explanations were tested and **disproved**, so nobody has to re-investigate them:

| Suspicion | How it was ruled out |
| --- | --- |
| Java heap growing unbounded | `-Xmx512m` was already in force and honoured. `jcmd GC.heap_info` showed the heap **using only 121 MB of its 512 MB cap** while RSS was 1.9 GB. |
| Leaked `MpvPlayer` handles (precache / crossfade) | Each live handle owns an `Mpv-Event-Pump` thread, so handles are directly countable. Over a 30-minute, 60-sample run the count **stayed flat at 3** and thread totals oscillated (117–158) without accumulating. Handles are released correctly. |

## What it actually was

**Allocator fragmentation.** The native memory had been `free()`d correctly — the C allocator was
simply holding onto the pages instead of returning them to the OS.

The decisive test was calling `malloc_trim(0)` on the running process:

```
RSS before : 920 MB
$1 = 1              <- glibc reports it released memory
RSS after  : 753 MB <- 167 MB handed straight back to the OS
```

Memory that can be returned on demand was, by definition, already free. That is fragmentation, not
a leak.

On Linux the amplifier was glibc's per-thread arenas:

```
nproc = 20  ->  glibc arena ceiling = 8 x nproc = 160
anon mappings aligned to a 64 MB boundary, counted = 161   (the arena signature)
RSS held by those mappings                          = 1451 MB
everything else (Skia, JNA, thread stacks, libs)    =  233 MB
```

glibc spawns a fresh arena whenever it sees allocator lock contention, up to `8 x nproc`, and never
gives one back. mpv/FFmpeg allocate and free large demuxer/decoder buffers on every track, and with
150+ threads in the process those allocations spread across every arena available.

**This is why RAM use depends on the user's core count** — a 4-core machine tops out at 32 arenas,
a 20-core machine at 160. It also explains why the three platforms differ in severity while sharing
the same symptom: Windows' NT heap and macOS' libmalloc have their own per-core caching, they just
scale less aggressively.

---

## The changes

Four changes across two repositories, each scoped to the platforms listed below.

| # | Change | File | Platforms |
| --- | --- | --- | --- |
| 1 | `MemoryTrimmer` — returns free pages to the OS when playback goes idle | `core/media/media-jvm/.../memory/MemoryTrimmer.kt` (new) + `.../mpv/MpvPlayerAdapter.kt` | Linux, Windows |
| 2 | Let G1 uncommit unused heap | `conveyor.conf` (`jvm` block) | all |
| 3 | Disable Apple's nano malloc zone | `conveyor.conf` (`mac` block) | macOS |
| 4 | Cap glibc arenas | `desktopApp/build.gradle.kts` (`AppRun`) | Linux |

Files 1 live in the **`core/` submodule**; the rest are in the parent repository.

### 1. `MemoryTrimmer`

Every desktop allocator can be asked to return free pages; only the spelling differs.

| OS | Call | Available since |
| --- | --- | --- |
| Linux | `malloc_trim(0)` | glibc |
| Windows | `HeapSetInformation(NULL, HeapOptimizeResources, ...)` | Windows 8.1 |

**macOS is deliberately excluded.** It does have an equivalent — `malloc_zone_pressure_relief(NULL, 0)`
— and it shipped briefly, but it was traced to a startup crash and removed. A null zone means *every
registered zone*, not just the one mpv/FFmpeg allocate from, so the call also asks the zones behind
Metal, QuartzCore and Skia to give pages back. Running off a background dispatcher it can land inside
a `CATransaction` commit on the main thread; the process then dies with an uncaught NSException
raised in `-[MTLLayer blitCallback]`, which macOS 26+ turns into a hard crash.

The window is narrow enough that anything perturbing timing hides it — attaching `log stream` was
enough — so it only reproduced on a real Finder/Dock launch, where activation and the window
animation hold the main thread inside CoreAnimation longer. Change 3 below already covers allocator
growth on macOS, so nothing is lost. **Do not re-add the macOS branch.**

Bound through JNA, which the module already uses for libmpv. Called from the `PAUSED` and `IDLE`
transitions in `MpvPlayerAdapter`, throttled to at most once per 60 s, dispatched off the service
thread.

**It must only run while idle.** `malloc_trim` walks the heap holding the allocator lock, so every
thread that calls `malloc` — mpv's decoder threads included — blocks until it returns. Calling it
during playback would be audible.

### 2. G1 heap uncommit

```
-XX:MinHeapFreeRatio=20
-XX:MaxHeapFreeRatio=40
-XX:G1PeriodicGCInterval=60000
```

Measured mid-session: G1 held **204 MB committed while only 104 MB was live**. `-Xmx` bounds how far
the heap may grow, not how much it hands back.

The periodic-GC flag is load-bearing, not decoration. G1 only uncommits at the end of a concurrent
cycle or a full GC, and a music player allocates so little that neither ever fires — the heap simply
ratchets to its high-water mark and stays. **Setting the free-ratio bounds without
`G1PeriodicGCInterval` is a no-op.**

### 3. macOS nano zone

```
info-plist.LSEnvironment.MallocNanoZone = "0"
```

The nano zone serves sub-256-byte allocations from its own region and has a history of not
coalescing what it frees. Electron and Chrome both hit this.

Two properties of `LSEnvironment` worth knowing:

- It is applied by **LaunchServices**, so it covers Finder / Dock / Spotlight / `open` launches —
  every normal launch of an installed `.app` — but **not** running `Contents/MacOS/...` directly
  from a shell.
- Declaring it **pins `PATH`** to the bare `/usr/bin:/bin:/usr/sbin:/sbin`. This was checked before
  adding: the only `ProcessBuilder` use on the macOS path is the Windows-only `powershell`/`wmic` VM
  probe, and link opening goes through `Desktop.browse()`, which LaunchServices resolves without
  consulting `PATH`. **Re-check this if the macOS build ever shells out to a binary outside those
  four directories.**

### 4. glibc arena cap (Linux)

```sh
export MALLOC_ARENA_MAX=2
```

Added to the generated `AppRun`, so it is Linux-only by construction — that file only exists inside
the AppImage.

Measured effect: arenas **161 → 5**, and RSS over a comparable session went from 1.90 GB to roughly
0.93 GB. The cost is more allocator lock contention, which this workload does not appear to notice:
the audio path is native and its buffers are long-lived.

### Deliberately not changed

- **Windows Segment Heap.** Microsoft's docs state the segment heap "has, by default, backed all
  process heaps for packaged apps since its inception", and this project ships Windows **only** as
  `.msix` — so it always runs with package identity and is already opted in. The
  `heap:HeapPolicy` manifest element is an opt-**out**. Worth confirming once on a real build with
  System Informer's heap-flags column or WinDbg `!heap`.
- **`demuxer-max-bytes`.** Looks like an easy 32 MB per handle, but it is a *ceiling*, not an
  allocation. With `cache-secs = 10`, a 320 kbps stream prefetches about **400 KB** — the ceiling is
  never approached, so lowering it would save nothing and only risk underruns.
- **Coil's in-memory cache.** Left at its default.

---

## Rollback

Each change is independent. Undo only the one whose symptom you see.

### Symptom → suspect

| Symptom | Suspect | Go to |
| --- | --- | --- |
| Audio glitches or stutters **when pausing / stopping** | `MemoryTrimmer` | A |
| macOS: hard crash a few seconds after a Finder/Dock launch, stack inside `-[MTLLayer blitCallback]` | `MemoryTrimmer`'s macOS branch is back | A |
| General sluggishness on Linux, higher CPU under load | `MALLOC_ARENA_MAX=2` (lock contention) | D |
| CPU spikes while the app sits idle | `G1PeriodicGCInterval` | B |
| macOS feels slower overall | `MallocNanoZone=0` | C |
| macOS: "command not found" / a helper process fails to launch | `LSEnvironment` pinned `PATH` | C |

### A. Disable `MemoryTrimmer`

Least invasive first — comment out the two call sites in `MpvPlayerAdapter.transitionToState`
(submodule `core/`); the class can stay:

```kotlin
InternalState.PAUSED -> {
    ...
    // trimNativeMemory("paused")
}
InternalState.IDLE -> {
    ...
    // trimNativeMemory("idle")
}
```

To keep trimming but make it rarer, raise `MIN_INTERVAL_MS` in `MemoryTrimmer` instead. To remove it
entirely: delete `core/media/media-jvm/.../memory/MemoryTrimmer.kt`, plus the import, the
`trimNativeMemory` helper and both calls in `MpvPlayerAdapter.kt`.

### B. Revert G1 heap flags

In `conveyor.conf`, delete these three lines from the `jvm` block. **Keep `-Xmx512m`** — that is
older and unrelated:

```
options += "-XX:MinHeapFreeRatio=20"
options += "-XX:MaxHeapFreeRatio=40"
options += "-XX:G1PeriodicGCInterval=60000"
```

### C. Revert the macOS nano zone

In `conveyor.conf`, delete this line from the `mac` block:

```
info-plist.LSEnvironment.MallocNanoZone = "0"
```

Removing it also restores normal `PATH` inheritance, so this is the fix for both macOS symptoms
above. If you want the memory benefit but need `PATH` back, keep the line and add `PATH` explicitly
to the same dict rather than dropping it.

### D. Revert or relax the arena cap

In `desktopApp/build.gradle.kts`, inside the `AppRun` heredoc, remove:

```sh
export MALLOC_ARENA_MAX=2
```

Prefer **relaxing before removing**: `2` is the aggressive end. Try `4`, then `8`. Even `8` is far
below the 160 this machine class defaults to, and it keeps most of the benefit while cutting
contention.

### Revert everything

```bash
git -C core checkout -- media/media-jvm/src/main/java/com/simpmusic/media_jvm/mpv/MpvPlayerAdapter.kt
rm -rf core/media/media-jvm/src/main/java/com/simpmusic/media_jvm/memory
git checkout -- conveyor.conf desktopApp/build.gradle.kts
```

(Adjust if these changes have already been committed — then it is a revert of those commits instead.)

---

## Measuring again

Take the reading **after listening for ~30 minutes**, not at startup. The whole point is behaviour
over a session.

**Linux**

```bash
pid=$(pgrep -f -i simpmusic | while read p; do [ "$(cat /proc/$p/comm)" = simpmusic ] && echo $p; done | head -1)
grep VmRSS /proc/$pid/status
jcmd $pid GC.heap_info
# count glibc arenas (64 MB-aligned anonymous mappings)
awk '/^[0-9a-f]/{split($1,a,"-"); if ($6=="") print a[1]}' /proc/$pid/maps |
  while read x; do python3 -c "print(1 if int('$x',16)%(64*1024*1024)==0 else 0)"; done | grep -c 1
```

**macOS**

```bash
pid=$(pgrep -f SimpMusic | head -1)
ps -o rss= -p $pid | awk '{print "RSS: "int($1/1024)" MB"}'
vmmap -summary $pid | head -25
jcmd $pid GC.heap_info
```

**Windows (PowerShell)**

```powershell
Get-Process SimpMusic | Select-Object Name,Id,@{n="WorkingSet_MB";e={[int]($_.WorkingSet64/1MB)}}
jcmd <pid> GC.heap_info
```

### Reading the result

The number that matters is **the ratio of heap to RSS**, not RSS alone.

- Heap small (~150 MB) but RSS large → native memory dominates; the changes above are the relevant
  lever.
- Heap large, close to the 512 MB cap → a Java-side problem instead; allocator tuning will not help
  and the investigation should start from `GC.heap_info` and a heap dump.

### Reference points (before any of these changes)

| Platform | RSS | Notes |
| --- | --- | --- |
| Linux, 20 cores, 1h40m session | 1.90 GB | heap using 121 MB; 161 arenas holding 1451 MB |
| Linux, with `MALLOC_ARENA_MAX=2`, 19 min | 0.93 GB | 5 arenas; still grew ~11 MB/min |
| Linux, after a manual `malloc_trim(0)` | 0.75 GB | 167 MB returned instantly |
| macOS / Windows | ~1 GB | reported; not yet broken down |
