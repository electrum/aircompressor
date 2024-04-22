#!/bin/bash

set -euo pipefail

RESOURCES=$(dirname $0)/../src/main/resources/aircompressor

download() {
    echo -n "Download $3 ... "
    OUT="$RESOURCES/$3"
    if [ -f "$OUT" ]; then
        echo "skipped"
        return
    fi
    TEMP=$(mktemp)
    curl -sSL "$1" | tar -xO data.tar.xz | tar -xO ".$2" > "$TEMP"
    mv -f "$TEMP" "$OUT"
    echo "downloaded"
}

# Snappy
download \
  "http://http.us.debian.org/debian/pool/main/s/snappy/libsnappy1v5_1.1.10-1+b1_amd64.deb" \
  "/usr/lib/x86_64-linux-gnu/libsnappy.so.1.1.10" \
  "Linux-amd64/libsnappy.so"

download \
  "http://http.us.debian.org/debian/pool/main/s/snappy/libsnappy1v5_1.1.10-1+b1_arm64.deb" \
  "/usr/lib/aarch64-linux-gnu/libsnappy.so.1.1.10" \
  "Linux-aarch64/libsnappy.so"

download \
  "http://http.us.debian.org/debian/pool/main/s/snappy/libsnappy1v5_1.1.10-1+b1_ppc64el.deb" \
  "/usr/lib/powerpc64le-linux-gnu/libsnappy.so.1.1.10" \
  "Linux-ppc64le/libsnappy.so"

# Zstandard
download \
  "http://http.us.debian.org/debian/pool/main/libz/libzstd/libzstd1_1.5.5+dfsg2-2_amd64.deb" \
  "/usr/lib/x86_64-linux-gnu/libzstd.so.1.5.5" \
  "Linux-amd64/libzstd.so"

download \
  "http://http.us.debian.org/debian/pool/main/libz/libzstd/libzstd1_1.5.5+dfsg2-2_arm64.deb" \
  "/usr/lib/aarch64-linux-gnu/libzstd.so.1.5.5" \
  "Linux-aarch64/libzstd.so"

download \
  "http://http.us.debian.org/debian/pool/main/libz/libzstd/libzstd1_1.5.5+dfsg2-2_ppc64el.deb" \
  "/usr/lib/powerpc64le-linux-gnu/libzstd.so.1.5.5" \
  "Linux-ppc64le/libzstd.so"

# LZ4
download \
  "http://http.us.debian.org/debian/pool/main/l/lz4/liblz4-1_1.9.4-2_amd64.deb" \
  "/usr/lib/x86_64-linux-gnu/liblz4.so.1.9.4" \
  "Linux-amd64/liblz4.so"

download \
  "http://http.us.debian.org/debian/pool/main/l/lz4/liblz4-1_1.9.4-2_arm64.deb" \
  "/usr/lib/aarch64-linux-gnu/liblz4.so.1.9.4" \
  "Linux-aarch64/liblz4.so"

download \
  "http://http.us.debian.org/debian/pool/main/l/lz4/liblz4-1_1.9.4-2_ppc64el.deb" \
  "/usr/lib/powerpc64le-linux-gnu/liblz4.so.1.9.4" \
  "Linux-ppc64le/liblz4.so"

# bzip2
download \
  "http://http.us.debian.org/debian/pool/main/b/bzip2/libbz2-1.0_1.0.8-5.1_amd64.deb" \
  "/usr/lib/x86_64-linux-gnu/libbz2.so.1.0.4" \
  "Linux-amd64/libbz2.so"

download \
  "http://http.us.debian.org/debian/pool/main/b/bzip2/libbz2-1.0_1.0.8-5.1_arm64.deb" \
  "/usr/lib/aarch64-linux-gnu/libbz2.so.1.0.4" \
  "Linux-aarch64/libbz2.so"

download \
  "http://http.us.debian.org/debian/pool/main/b/bzip2/libbz2-1.0_1.0.8-5.1_ppc64el.deb" \
  "/usr/lib/powerpc64le-linux-gnu/libbz2.so.1.0.4" \
  "Linux-ppc64le/libbz2.so"
