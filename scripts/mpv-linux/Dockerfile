# Builds a REAL libmpv.so.2 shared library against glibc 2.35 (Ubuntu 22.04).
#
# Why not reuse the upstream mpv AppImage: that bundle ships its own glibc and
# loader because it is meant to run mpv as its own process. We dlopen libmpv
# into a JVM that already has the system glibc mapped, and two glibcs cannot
# coexist in one process. The AppImage binary is also a PIE executable, which
# glibc refuses to dlopen outright.
#
# Ubuntu 22.04 is the oldest base that still builds mpv 0.41 without heroics,
# so the resulting slice runs on Ubuntu 22.04 / Debian 12 and newer.
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Build toolchain. meson from apt is 0.61 (mpv needs >=1.3), so it comes from pip.
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential git ca-certificates curl pkg-config python3 python3-pip \
        ninja-build nasm yasm patchelf \
        libass-dev libfreetype6-dev libfribidi-dev libharfbuzz-dev \
        libfontconfig1-dev \
        libasound2-dev libpulse-dev \
        libxml2-dev zlib1g-dev libbz2-dev liblzma-dev \
        libssl-dev \
        libdav1d-dev \
    && rm -rf /var/lib/apt/lists/* \
    && pip3 install --no-cache-dir "meson>=1.3.0"

WORKDIR /build

# ---------------------------------------------------------------------------
# libplacebo — mpv 0.41 hard-requires >=6.338.2; 22.04 ships 4.192.
#
# Built WITHOUT vulkan/shaderc/glslang/d3d11. Those exist to compile shaders for
# hardware renderers; SimpMusic drives mpv through the software render API
# (MPV_RENDER_API_TYPE_SW), so none of it is reachable. Dropping them also drops
# libshaderc/libglslang/libSPIRV-Tools from the closure — the single biggest
# chunk of the old AppImage-derived bundle.
# ---------------------------------------------------------------------------
RUN git clone --depth 1 --branch v7.351.0 --recursive \
        https://code.videolan.org/videolan/libplacebo.git && \
    cd libplacebo && \
    meson setup build \
        --prefix=/usr/local \
        --buildtype=release \
        -Ddefault_library=shared \
        -Dvulkan=disabled -Dopengl=disabled -Dd3d11=disabled \
        -Dshaderc=disabled -Dglslang=disabled \
        -Ddemos=false -Dtests=false && \
    ninja -C build -j$(nproc) && ninja -C build install && \
    cd .. && rm -rf libplacebo

# ---------------------------------------------------------------------------
# FFmpeg — 22.04 ships 4.4, mpv 0.41 needs libavcodec >=60.31 (FFmpeg 6.1+).
#
# Decoders/demuxers are trimmed to what YouTube Music actually serves, plus the
# protocol stack for https. Encoders are dropped entirely: SimpMusic only ever
# plays back.
# ---------------------------------------------------------------------------
RUN git clone --depth 1 --branch n7.1.1 https://github.com/FFmpeg/FFmpeg.git ffmpeg && \
    cd ffmpeg && \
    ./configure \
        --prefix=/usr/local \
        --enable-shared --disable-static \
        --disable-programs --disable-doc --disable-debug \
        --disable-encoders --disable-muxers --disable-devices \
        --disable-filters --enable-filter=aresample,atempo,rubberband,scale,format,aformat,anull,null,volume,lavfi,acompressor,firequalizer,equalizer,highpass,lowpass,dynaudnorm,loudnorm,aecho \
        --enable-openssl --enable-protocol=https,http,tls,file,pipe,crypto,hls,data \
        --enable-libdav1d \
        --enable-version3 \
    && make -j$(nproc) && make install && \
    cd .. && rm -rf ffmpeg

# ---------------------------------------------------------------------------
# mpv itself — the only artifact that matters is libmpv.so.2.
#
# libmpv is what we load; the mpv CLI player is not built. Video output goes
# through the software render API, so X11/Wayland/GPU backends are all off.
# ---------------------------------------------------------------------------
RUN git clone --depth 1 --branch v0.41.0 https://github.com/mpv-player/mpv.git && \
    cd mpv && \
    PKG_CONFIG_PATH=/usr/local/lib/x86_64-linux-gnu/pkgconfig:/usr/local/lib/pkgconfig \
    meson setup build \
        --prefix=/usr/local \
        --buildtype=release \
        -Dlibmpv=true -Dcplayer=false \
        -Dgpl=false \
        -Dx11=disabled -Dwayland=disabled -Ddrm=disabled -Dgbm=disabled \
        -Degl=disabled -Dgl=disabled -Dvulkan=disabled \
        -Dalsa=enabled -Dpulse=enabled \
        -Dlua=disabled -Djavascript=disabled \
        -Dlibarchive=disabled -Dlibbluray=disabled -Ddvdnav=disabled \
        -Dcdda=disabled -Duchardet=disabled \
        -Drubberband=disabled -Dzimg=disabled \
        -Dmanpage-build=disabled && \
    ninja -C build -j$(nproc) && ninja -C build install

# Stage libmpv + its non-system closure into /out, laid out the way
# mpv-natives/linux-x64/ expects: libmpv.so.2 at the top, everything else in lib/.
COPY stage.sh /stage.sh
RUN bash /stage.sh
