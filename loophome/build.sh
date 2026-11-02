#!/bin/bash
# Loop Home — Gradle'sız derleme: aapt2 → javac → d8 → zipalign → apksigner.
#
#   ./build.sh            derle ve imzala (build/LoopHome.apk)
#   ./build.sh --install  derle, imzala, TV'ye kur
#   ./build.sh --home     derle, imzala, kur ve ana ekranı yeniden başlat
#   ./build.sh --check    sadece derle (imzasız), kod hatası var mı diye bakmak için
#
# Gizli dosyalar git'e girmez; LOOPHOME_SECRETS klasöründe durur (varsayılan: loophome/local/):
#   loophome.keystore   imza anahtarı (yoksa ilk derlemede oluşturulur — SAKLA, kaybolursa
#                       güncelleme kurulamaz, uygulamayı silip baştan kurmak gerekir)
#   keystore.pass       keystore şifresi (tek satır)
#   keys.properties     isteğe bağlı: unsplash_key=... (APK'ya gömülür; setup.sh ile cihaza
#                       göndermek daha iyidir, o zaman bu dosyaya gerek yok)
#   tv.conf             isteğe bağlı: TV=192.168.x.y:5555
set -euo pipefail

cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$(ls -d "$SDK"/build-tools/*/ 2>/dev/null | sort -V | tail -1)"
JAR="$SDK/platforms/android-34/android.jar"
SECRETS="${LOOPHOME_SECRETS:-$PWD/local}"
KS="$SECRETS/loophome.keystore"
[ -f "$SECRETS/tv.conf" ] && . "$SECRETS/tv.conf"
TV="${TV:-}"
MODE="${1:-}"

if [ -z "$BT" ] || [ ! -f "$JAR" ]; then
    echo "Android SDK bulunamadı: build-tools ve platforms/android-34 gerekli (ANDROID_HOME=$SDK)" >&2
    exit 1
fi

rm -rf build && mkdir -p build/res build/gen build/classes build/assets build/dex

echo "• kaynaklar"
"$BT/aapt2" compile --dir res -o build/res/res.zip
"$BT/aapt2" link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml \
    --java build/gen -A assets build/res/res.zip

echo "• java"
find src build/gen -name '*.java' > build/sources.txt
javac -nowarn -encoding UTF-8 --release 11 -cp "$JAR" -d build/classes @build/sources.txt

echo "• dex"
"$BT/d8" --release --min-api 30 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')

echo "• paketle"
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q -0 ../unsigned.apk classes.dex)
if [ -f "$SECRETS/keys.properties" ] && grep -qE '^unsplash_key=.+' "$SECRETS/keys.properties"; then
    grep -E '^unsplash_key=' "$SECRETS/keys.properties" > build/assets/config.properties
    (cd build && zip -q -0 unsigned.apk assets/config.properties)
    echo "  Unsplash anahtarı APK'ya eklendi"
fi
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk

if [ "$MODE" = "--check" ]; then
    echo "✓ derleme tamam (imzasız): build/aligned.apk"
    exit 0
fi

if [ ! -f "$KS" ]; then
    echo "• imza anahtarı yok, oluşturuluyor: $KS"
    mkdir -p "$SECRETS"
    [ -f "$SECRETS/keystore.pass" ] || LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 24 > "$SECRETS/keystore.pass"
    chmod 600 "$SECRETS/keystore.pass"
    keytool -genkeypair -keystore "$KS" -storepass "$(cat "$SECRETS/keystore.pass")" -alias loophome \
        -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Loop Home" >/dev/null 2>&1
    echo "  ⚠ $KS dosyasını yedekle: kaybolursa güncelleme kurulamaz"
fi

echo "• imzala"
PASS_ARGS=()
if [ -n "${LOOPHOME_KS_PASS:-}" ]; then
    PASS_ARGS=(--ks-pass env:LOOPHOME_KS_PASS)
elif [ -f "$SECRETS/keystore.pass" ]; then
    PASS_ARGS=(--ks-pass "file:$SECRETS/keystore.pass")
fi
"$BT/apksigner" sign --ks "$KS" "${PASS_ARGS[@]}" --out build/LoopHome.apk build/aligned.apk
echo "✓ build/LoopHome.apk"

if [ "$MODE" = "--install" ] || [ "$MODE" = "--home" ]; then
    if [ -z "$TV" ]; then
        echo "TV adresi yok: TV=192.168.x.y:5555 ./build.sh $MODE  (ya da $SECRETS/tv.conf)" >&2
        exit 1
    fi
    adb -s "$TV" install -r build/LoopHome.apk
    # Android, güncellemeden kısa süre SONRA Loop Home'un erişilebilirlik servisini listeden
    # düşürüyor (hemen yazılan değeri de eziyor); bekle, ekle, tuttuğunu doğrula.
    SVC="com.mustycetin.loophome/com.mustycetin.loophome.KeyService"
    for try in 1 2 3 4 5; do
        sleep 2
        CUR="$(adb -s "$TV" shell settings get secure enabled_accessibility_services | tr -d '\r')"
        case "$CUR" in
            *"$SVC"*) [ "$try" -gt 1 ] && break ;;
            ""|null) adb -s "$TV" shell settings put secure enabled_accessibility_services "$SVC" ;;
            *) adb -s "$TV" shell settings put secure enabled_accessibility_services "$SVC:$CUR" ;;
        esac
        adb -s "$TV" shell settings put secure accessibility_enabled 1
    done
    case "$(adb -s "$TV" shell settings get secure enabled_accessibility_services)" in
        *"$SVC"*) echo "  erişilebilirlik servisi açık" ;;
        *) echo "  ⚠ erişilebilirlik servisi eklenemedi; elle: settings put secure enabled_accessibility_services $SVC" >&2 ;;
    esac
fi
if [ "$MODE" = "--home" ]; then
    adb -s "$TV" shell am force-stop com.mustycetin.loophome
    adb -s "$TV" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
fi
