# Listen Together — working notes

Issue: [#2344](https://github.com/maxrave-dev/SimpMusic/issues/2344) · Branch `feat/listen-together`
(in **both** repos — parent and the `core` submodule) · Design canvas:
https://claude.ai/code/artifact/251bf24d-09b9-4bdc-9c51-b7fd9d4a718d

**The hard constraint: SimpMusic clients must share rooms with Metrolist clients on the same
servers.** Nothing in the protocol layer may be "improved" — a renamed field, a reordered proto
number, a tightened parser is a client that silently cannot join.

---

## Where the truth lives

| What | Where | Note |
|---|---|---|
| Wire schema | `MetrolistGroup/metroproto` → `listentogether.proto` | 240 lines, proto3. A git submodule of Metrolist, **not** in its main tree — easy to miss |
| Reference client | `MetrolistGroup/Metrolist` → `app/.../listentogether/` | `Manager` 96 KB, `Client` 86 KB, `MessageCodec` 15 KB, `Protocol` 9 KB |
| Reference UI | `.../ui/screens/ListenTogetherScreen.kt` | 1471 lines, 12 composables |
| Servers | one, public | `wss://metroserverx.meowery.eu/ws` — Poland, operator "Nyx". Many Metrolist forks already use it; no permission needed |

## The wire format, in one paragraph

Payload → protobuf → gzip **if over 100 bytes** → wrapped in `Envelope { type, payload, compressed }`
→ the envelope is protobuf too. First message on the socket is `ClientCapabilities`
(`supports_protobuf`, `supports_compression`, `client_version`) and the server answers
`ServerCapabilities`. That handshake is the protocol's **only** version signal.

Synchronisation is a **buffer barrier**, not a blind seek: on a track change every client answers
`buffer_ready`, the server holds everyone in `buffer_wait` until the slowest one does, then releases
with `buffer_complete`. Position correctness rides on `ServerClock` — ping/pong estimates the offset
between the server's wall clock and the device's monotonic clock, and `positionAt()` advances a
position by however long the command spent in flight.

---

## Done on this branch

`core/service/listenTogether` — a new KMP module (android + jvm + ios), wired into
`settings.gradle.kts`.

- `Protocol.kt` — every message from the `.proto` as `@Serializable` + `@ProtoNumber`, plus
  `MessageTypes` (36 constants) and `PlaybackActions` (11).
- `ServerClock.kt` — ported; `@Synchronized` became atomicfu's `SynchronizedObject`.
- `MessageCodec.kt` — envelope encode/decode, gzip through okio.
- `MessageCodecTest` / `ServerClockTest` — ported, plus four extra cases (compression, unknown type,
  chat tolerance, envelope tag bytes).

New dependencies, both first-party Kotlin: `kotlinx-serialization-protobuf`, `atomicfu`.

### Two decisions worth not re-litigating

**No protoc.** Metrolist generates Java from the `.proto` with `protoc`, which is JVM-only and would
strand Desktop and iOS. `kotlinx-serialization-protobuf` emits the same wire bytes from
`@ProtoNumber` annotations, so the codec lives in `commonMain`. That equivalence is an assumption,
and `MessageCodecTest.envelopeFieldNumbersMatchTheProtoSchema` is what pins it — if it ever drifts,
that test fails before a user does.

**No `toProtoMessage` / `protoToX`.** Roughly 250 of `MessageCodec`'s 375 lines exist only to bridge
hand-written Kotlin classes to protoc's Java builders. With annotated classes there is no gap to
bridge, so they are simply absent. Same bytes, one model.

---

## Next, in order

1. **`ListenTogetherClient`** — Ktor WebSocket transport: connect, capabilities handshake, send/receive
   frames, ping loop, reconnect with `session_token`. Needs `ktor-client-websockets` added to the
   version catalog (CIO and OkHttp both already support it).
2. **`ListenTogetherSession` in `commonMain`** — the state machine. **This is the hard part**, and it
   is a rewrite rather than a port: Metrolist's 96 KB manager is wired straight into its single
   Android `MusicService`, while SimpMusic runs two entirely separate handlers.
3. **Wire into both player handlers** — `MediaServiceHandlerImpl` (Android) and
   `JvmMediaPlayerHandlerImpl` (Desktop).
4. **UI** — see the design canvas. Five surfaces: lobby, room-as-host, room-as-guest, settings, and
   the top-app-bar entry.
5. **Settings + strings.**

### Traps already identified

- **`buffer_ready` must mean the same thing on both platforms.** `MpvPlayerAdapter` and
  `CrossfadeExoPlayerAdapter` have different seek latencies and different notions of "buffered
  enough". If Desktop answers ready sooner than Android, a mixed room desyncs — and the failure is
  silent, it just drifts.
- **Crossfade must be off inside a room**, and the guard belongs on **both** trigger paths — the
  position-polling job *and* `handleTrackEndInternal()`. A guard on only one is dead code with no
  symptom. (Same shape as the bug in `CLAUDE.md` → "Every crossfade guard belongs on BOTH trigger
  paths".)
- **Tolerate `chat`.** SimpMusic draws no chat UI, but a Metrolist user in the room will send it.
  `MessageCodec.decodePayload` returns null for unknown types instead of throwing, for the same
  reason.
- **The entry point is the top app bar of Home and Library**, opening a new screen — decided, and
  deliberately *not* what Metrolist does (they made it a nav tab, `Screens.MainScreens`). SimpMusic
  already has Home / Mix / Analytics / Library plus the Search FAB; a sixth tab is too many. The
  badge (people count, or an amber dot for pending approvals) is what keeps a top-bar icon from
  being buried the way the old Analytics icon was.

### Open question for the owner

Suggesting a track — reuse the existing Search screen, or a dedicated sheet?

---

## Attribution

The protocol, codec and clock are ported from
[Metrolist](https://github.com/MetrolistGroup/Metrolist), GPL-3.0, the same licence as this project.
Copyright notices are preserved in each ported file and must stay.
