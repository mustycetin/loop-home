#!/bin/bash
# Osplay'i (github.com/akinalpfdn/osplay) 32-bit / Android 11 TV kutuları için derler.
#
# Resmi APK sadece arm64 + Android 12 (minSdk 31) için. Kod Android 11'de çalışıyor
# (H.264 çözme + AES şifreleme, Android 12'ye özel API yok; lint de NewApi hatası vermiyor),
# bu yüzden iki ayar yetiyor: minSdk 31 → 30 ve abiFilters → armeabi-v7a.
# Xiaomi TV Box S 2. nesilde (MiTV-AFKR0) v0.2.0 ile denendi, sorunsuz çalışıyor.
#
#   ./tools/osplay-32bit.sh [sürüm]      (varsayılan: v0.2.0)
#
# Gerekenler: git, JDK 17, Android SDK (ANDROID_HOME), cmake ve ninja (macOS: brew install cmake ninja).
# NDK'yı Gradle kendisi indirir. Çıktı, debug anahtarıyla imzalı küçültülmüş APK'dır.
set -euo pipefail

TAG="${1:-v0.2.0}"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
WORK="$(mktemp -d)/osplay"
CMAKE="$(command -v cmake || true)"
[ -n "$CMAKE" ] && command -v ninja >/dev/null || { echo "cmake ve ninja gerekli (brew install cmake ninja)"; exit 1; }
CMAKE_VER="$(cmake --version | head -1 | awk '{print $3}')"
CMAKE_DIR="$(cd "$(dirname "$CMAKE")/.." && pwd)"

git clone -q --depth 1 --branch "$TAG" https://github.com/akinalpfdn/osplay "$WORK"
cd "$WORK/android"
perl -pi \
    -e 's/minSdk = 31/minSdk = 30/;' \
    -e 's/abiFilters \+= listOf\("arm64-v8a", "x86_64"\)/abiFilters += listOf("armeabi-v7a")/;' \
    -e "s/version = \"3\\.[0-9.]*\"/version = \"$CMAKE_VER\"/;" \
    -e 's/^(\s*)isMinifyEnabled = true/$1signingConfig = signingConfigs.getByName("debug")\n$1isMinifyEnabled = true/;' \
    app/build.gradle.kts
grep -q 'minSdk = 30' app/build.gradle.kts || { echo "build.gradle.kts beklenen biçimde değil; $TAG için tarifi güncelle"; exit 1; }
printf 'sdk.dir=%s\ncmake.dir=%s\n' "$SDK" "$CMAKE_DIR" > local.properties
./gradlew --no-daemon -q assembleRelease
OUT="$(pwd)/app/build/outputs/apk/release/app-release.apk"
cp "$OUT" "$OLDPWD/Osplay-$TAG-armeabi-v7a.apk" 2>/dev/null || true
echo "✓ $OUT"
echo "Kur: adb -s <TV_IP>:5555 install -r \"$OUT\""
