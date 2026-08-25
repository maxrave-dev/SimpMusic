#!/usr/bin/env bash
# Collect libmpv.so.2 and every non-system shared object it needs into /out.
#
# "System" here means the set every glibc-based desktop is guaranteed to have.
# Bundling those would be actively harmful: the JVM has already mapped the
# host's libc/libm/libpthread, and a second copy in one process is exactly the
# failure that killed the AppImage approach.
set -euo pipefail

OUT=/out
mkdir -p "$OUT/lib"

# libplacebo and FFmpeg installed into /usr/local/lib, which is not in the
# loader cache on a bare Ubuntu image. Without this, ldd reports every
# libav*/libplacebo as "not found" and they silently miss the closure — the
# staged slice then looks complete but cannot load.
echo "/usr/local/lib" > /etc/ld.so.conf.d/local.conf
echo "/usr/local/lib/x86_64-linux-gnu" >> /etc/ld.so.conf.d/local.conf
ldconfig

SYSTEM_LIBS="
libc.so.6 libm.so.6 libdl.so.2 libpthread.so.0 librt.so.1 libutil.so.1
ld-linux-x86-64.so.2 libgcc_s.so.1 libstdc++.so.6 libresolv.so.2
libz.so.1 libbz2.so.1.0 liblzma.so.5 libglib-2.0.so.0
"

is_system() {
  local name="$1"
  for s in $SYSTEM_LIBS; do [[ "$name" == "$s" ]] && return 0; done
  return 1
}

LIBMPV=$(find /usr/local/lib -name 'libmpv.so.2*' -type f | head -1)
[[ -n "$LIBMPV" ]] || { echo "libmpv.so.2 not found after install"; exit 1; }
cp "$LIBMPV" "$OUT/libmpv.so.2"
chmod +w "$OUT/libmpv.so.2"

# Walk the DT_NEEDED graph. ldd resolves transitively already, so one pass over
# its output is enough — but ldd also lists the system libs, hence the filter.
collect() {
  ldd "$1" 2>/dev/null | awk '/=>/ {print $1, $3}' | while read -r name path; do
    [[ -z "$path" || "$path" == "not" ]] && continue
    is_system "$name" && continue
    [[ -f "$OUT/lib/$name" ]] && continue
    cp -L "$path" "$OUT/lib/$name"
    chmod +w "$OUT/lib/$name"
    collect "$OUT/lib/$name"
  done
}
collect "$OUT/libmpv.so.2"

# DT_RPATH, not DT_RUNPATH: RUNPATH applies only to the object that carries it,
# so a dependency-of-a-dependency would not be found. RPATH is inherited down
# the whole chain, which is what lets one entry here cover the entire closure.
patchelf --force-rpath --set-rpath '$ORIGIN/lib' "$OUT/libmpv.so.2"
for so in "$OUT"/lib/*.so*; do
  patchelf --force-rpath --set-rpath '$ORIGIN' "$so"
done

echo "=== staged ==="
ls -la "$OUT"
echo "--- lib/ ($(ls "$OUT/lib" | wc -l) objects, $(du -sh "$OUT/lib" | cut -f1)) ---"
ls "$OUT/lib"
# Fail here, in the builder, rather than on a user's machine. A slice that looks
# complete but cannot resolve its own closure is exactly what shipped before.
echo "=== resolution check ==="
missing=$(ldd "$OUT/libmpv.so.2" 2>/dev/null | grep "not found" || true)
for so in "$OUT"/lib/*.so*; do
  missing+=$(ldd "$so" 2>/dev/null | grep "not found" || true)
done
if [[ -n "$missing" ]]; then
  echo "UNRESOLVED dependencies remain:"
  echo "$missing" | sort -u
  exit 1
fi
echo "(all resolved via \$ORIGIN — no LD_LIBRARY_PATH needed)"

# Prove the staged library actually loads and initialises, in this container,
# with nothing but $ORIGIN to find its dependencies.
cat > /tmp/smoke.c <<'EOF'
#include <stdio.h>
#include <dlfcn.h>
int main(void) {
    void *h = dlopen("/out/libmpv.so.2", RTLD_NOW | RTLD_LOCAL);
    if (!h) { printf("dlopen FAILED: %s\n", dlerror()); return 1; }
    unsigned long (*ver)(void) = dlsym(h, "mpv_client_api_version");
    void *(*create)(void) = dlsym(h, "mpv_create");
    int (*init)(void *) = dlsym(h, "mpv_initialize");
    if (!ver || !create || !init) { printf("dlsym FAILED\n"); return 1; }
    unsigned long v = ver();
    printf("client api = %lu.%lu\n", v >> 16, v & 0xffff);
    void *ctx = create();
    if (!ctx) { printf("mpv_create returned NULL\n"); return 1; }
    int rc = init(ctx);
    printf("mpv_initialize = %d %s\n", rc, rc == 0 ? "(OK)" : "(FAILED)");
    return rc == 0 ? 0 : 1;
}
EOF
gcc -o /tmp/smoke /tmp/smoke.c -ldl
echo "=== smoke test ==="
LC_NUMERIC=C /tmp/smoke
