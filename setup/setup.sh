#!/bin/bash
# Loop Home kurulumu — Xiaomi TV Box S (2. nesil) ve benzeri Google TV kutuları için.
#
#   ./setup/setup.sh             adım adım, her şeyi sorarak kurar
#   ./setup/setup.sh --dry-run   hiçbir şeyi değiştirmez, ne yapacağını gösterir
#
# Gerekenler: adb (macOS: brew install android-platform-tools), python3, curl.
# Loop Home'u kaynaktan derlemek için ayrıca Android SDK + JDK 17 (bkz. docs/KURULUM.md).
#
# Yapılan her değişiklik, geri alma komutuyla birlikte setup/kurulum-kaydi-*.txt dosyasına yazılır.
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
DRY=0
[ "${1:-}" = "--dry-run" ] && DRY=1
LOG="$HERE/kurulum-kaydi-$(date +%Y%m%d-%H%M%S).txt"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
LOOPHOME=com.mustycetin.loophome
KEYSVC="$LOOPHOME/$LOOPHOME.KeyService"

bold() { printf '\n\033[1m%s\033[0m\n' "$*"; }
say() { printf '%s\n' "$*"; }
warn() { printf '\033[33m⚠ %s\033[0m\n' "$*"; }

# ask "soru" E|H  → 0 evet, 1 hayır
ask() {
    local def="$2" ans hint="[e/H]"
    [ "$def" = "E" ] && hint="[E/h]"
    read -r -p "$1 $hint " ans </dev/tty
    ans="${ans:-$def}"
    [[ "$ans" =~ ^[EeYy] ]]
}

log() { printf '%s\n' "$*" >> "$LOG"; }

# TV'de değişiklik yapan komut. Kayda yazılır; --dry-run'da sadece gösterilir.
tv() {
    if [ "$DRY" = 1 ]; then
        say "  [deneme] adb shell $*" >&2
        return 0
    fi
    adb -s "$TV" shell "$@" </dev/null
}

# TV'den sadece okuyan komut.
tvq() { adb -s "$TV" shell "$@" </dev/null 2>/dev/null | tr -d '\r'; }

# Baştaki/sondaki boşlukları atar (xargs, açıklamalardaki kesme işaretinde bozuluyor).
trim() {
    local s="$1"
    s="${s#"${s%%[![:space:]]*}"}"
    s="${s%"${s##*[![:space:]]}"}"
    printf '%s' "$s"
}

done_step() { # açıklama, geri-alma-komutu
    log "- $1"
    [ -n "${2:-}" ] && log "    geri al: adb shell $2"
}

has_pkg() { grep -qx "package:$1" "$TMP/pkgs"; }
is_disabled() { grep -qx "package:$1" "$TMP/disabled"; }
refresh_pkgs() {
    tvq pm list packages > "$TMP/pkgs"
    tvq pm list packages -d > "$TMP/disabled"
}

# ---------------------------------------------------------------- bağlantı
bold "Loop Home kurulumu"
[ "$DRY" = 1 ] && warn "DENEME MODU: TV'de hiçbir şey değişmeyecek."
command -v adb >/dev/null || { say "adb bulunamadı. macOS: brew install android-platform-tools"; exit 1; }
command -v python3 >/dev/null || { say "python3 bulunamadı."; exit 1; }

say "TV'de: Ayarlar → Sistem → Hakkında → 'Android TV OS derlemesi'ne 7 kez bas (geliştirici modu),"
say "sonra Ayarlar → Sistem → Geliştirici seçenekleri → 'USB hata ayıklama'yı aç."
say "IP adresi: Ayarlar → Ağ ve İnternet → bağlı ağ."
read -r -p "TV'nin IP adresi (örn. 192.168.1.50): " IP </dev/tty
TV="$IP"
[[ "$TV" == *:* ]] || TV="$TV:5555"
adb connect "$TV" >/dev/null 2>&1
for i in $(seq 1 30); do
    state="$(adb -s "$TV" get-state 2>/dev/null || true)"
    [ "$state" = "device" ] && break
    [ "$i" = 1 ] && say "TV ekranında 'USB hata ayıklamaya izin ver' penceresini onayla (her zaman izin ver)…"
    sleep 2
    adb connect "$TV" >/dev/null 2>&1
done
[ "$(adb -s "$TV" get-state 2>/dev/null)" = "device" ] || { say "TV'ye bağlanılamadı ($TV)."; exit 1; }

MODEL="$(tvq getprop ro.product.model)"
SDK="$(tvq getprop ro.build.version.sdk)"
ABIS="$(tvq getprop ro.product.cpu.abilist)"
ABI=armeabi-v7a
[[ "$ABIS" == *arm64-v8a* ]] && ABI=arm64-v8a
say "Bağlandı: $MODEL, Android API $SDK, işlemci: $ABIS"
if [ "$MODEL" != "MiTV-AFKR0" ]; then
    warn "Bu kurulum Xiaomi TV Box S 2. nesil (MiTV-AFKR0) üzerinde denendi. Başka cihazda temizlik listesini dikkatli seç."
fi
{
    echo "# Loop Home kurulum kaydı — $(date)"
    echo "# Cihaz: $MODEL (API $SDK, $ABIS)"
    [ "$DRY" = 1 ] && echo "# DENEME MODU — hiçbir şey uygulanmadı"
    echo
} > "$LOG"
refresh_pkgs

# ---------------------------------------------------------------- 1. temizlik
bold "1) Gereksiz Xiaomi/Google uygulamalarını temizle"
say "Paketler cihazdan silinmez, sadece bu kullanıcı için kaldırılır/kapatılır; hepsi geri alınabilir."
read -r -p "[h]epsini uygula / [t]ek tek sor / [a]tla? [t] " MODE </dev/tty
MODE="${MODE:-t}"
if [[ "$MODE" =~ ^[HhTt] ]]; then
    while IFS='|' read -r op pkg desc; do
        op="$(trim "$op")"; pkg="$(trim "$pkg")"; desc="$(trim "$desc")"
        [ -z "$op" ] || [[ "$op" == \#* ]] && continue
        has_pkg "$pkg" || continue
        [ "$op" = kapat ] && is_disabled "$pkg" && continue
        trapnote="${desc#! }"
        if [ "$pkg" = "com.google.android.tungsten.setupwraith" ]; then
            warn "setupwraith: TV ilk kurulumda Wi-Fi'yi bu uygulamayla kaydettiyse, kaldırınca Wi-Fi gider."
            say  "  Önce Ayarlar → Ağ'dan Wi-Fi ağını UNUT ve yeniden bağlan; sonra kaldırmak güvenli."
            ask "  Wi-Fi'yi Ayarlar'dan yeniden bağladın mı? (bağlamadıysan atla)" H || continue
        elif [[ "$MODE" =~ ^[Tt] ]]; then
            verb="Kaldırılsın mı"
            [ "$op" = kapat ] && verb="Kapatılsın mı"
            ask "  $pkg — $trapnote. $verb?" E || continue
        fi
        if [ "$op" = "kaldir" ]; then
            tv pm uninstall -k --user 0 "$pkg" >/dev/null && say "  kaldırıldı: $pkg"
            done_step "kaldırıldı: $pkg ($trapnote)" "cmd package install-existing $pkg"
        else
            tv pm disable-user --user 0 "$pkg" >/dev/null && say "  kapatıldı: $pkg"
            done_step "kapatıldı: $pkg ($trapnote)" "pm enable $pkg"
        fi
    done < "$HERE/debloat.txt"
fi

# ---------------------------------------------------------------- 2. sistem ayarları
bold "2) Sistem ayarları"
if has_pkg com.xiaomi.mitv.updateservice && ! is_disabled com.xiaomi.mitv.updateservice && ask "Xiaomi sistem güncellemelerini kapat? (güncellemeler bu ayarları geri getirebilir)" E; then
    tv pm disable-user --user 0 com.xiaomi.mitv.updateservice >/dev/null
    done_step "Xiaomi güncelleme servisi kapatıldı" "pm enable com.xiaomi.mitv.updateservice"
fi
if ask "Reklam ve izleyici engelleyen AdGuard DNS'i aç? (Özel DNS: dns.adguard-dns.com)" E; then
    tv settings put global private_dns_mode hostname
    tv settings put global private_dns_specifier dns.adguard-dns.com
    done_step "Özel DNS: dns.adguard-dns.com" "settings put global private_dns_mode opportunistic"
fi
if ask "Arayüz animasyonlarını hızlandır (0,5x)?" E; then
    for s in window_animation_scale transition_animation_scale animator_duration_scale; do
        tv settings put global "$s" 0.5
    done
    done_step "Animasyon hızları 0.5" "settings put global window_animation_scale 1; settings put global transition_animation_scale 1; settings put global animator_duration_scale 1"
fi
if ask "Ekran koruyucuyu kapat? (Loop Home'un kendi fotoğrafları ve uyku zamanlayıcısı var)" E; then
    tv settings put secure screensaver_enabled 0
    done_step "Ekran koruyucu kapatıldı" "settings put secure screensaver_enabled 1"
fi

# ---------------------------------------------------------------- 3. Loop Home
bold "3) Loop Home ana ekranı"
if ask "Loop Home kurulsun mu?" E; then
    APK="${LOOPHOME_APK:-}"
    if [ -z "$APK" ] && [ -f "$ROOT/dist/LoopHome.apk" ]; then
        APK="$ROOT/dist/LoopHome.apk"
    fi
    if [ -z "$APK" ] || ask "Kaynaktan derleyeyim mi? (Android SDK gerekir; hayır = $APK)" H; then
        if [ "$DRY" = 1 ]; then
            say "  [deneme] loophome/build.sh"
        else
            "$ROOT/loophome/build.sh" && APK="$ROOT/loophome/build/LoopHome.apk"
        fi
    fi
    if [ "$DRY" = 1 ]; then
        say "  [deneme] adb install -r ${APK:-LoopHome.apk}"
    elif [ -n "$APK" ] && [ -f "$APK" ]; then
        adb -s "$TV" install -r "$APK" </dev/null | tail -1
    else
        say "  APK yok. LOOPHOME_APK=/yol/LoopHome.apk ile ya da Android SDK kurup tekrar dene."
    fi
    done_step "Loop Home kuruldu" "pm uninstall $LOOPHOME"

    # Açık Uygulamalar ekranı kullanım istatistiğini okur.
    tv appops set "$LOOPHOME" GET_USAGE_STATS allow
    done_step "Loop Home'a kullanım istatistiği izni" "appops set $LOOPHOME GET_USAGE_STATS default"
    # Home basılı tutma, TV+ tuşu, liste tuşu ve uyku zamanlayıcısı erişilebilirlik servisiyle çalışır.
    CUR="$(tvq settings get secure enabled_accessibility_services)"
    case "$CUR" in
        *"$KEYSVC"*) NEW="$CUR" ;;
        ""|null) NEW="$KEYSVC" ;;
        *) NEW="$KEYSVC:$CUR" ;;
    esac
    tv settings put secure enabled_accessibility_services "$NEW"
    tv settings put secure accessibility_enabled 1
    done_step "Loop Home erişilebilirlik servisi açıldı" "settings put secure enabled_accessibility_services '${CUR}'"

    if has_pkg com.google.android.apps.tv.launcherx && ! is_disabled com.google.android.apps.tv.launcherx; then
        say "Google TV'nin kendi ana ekranı kapatılırsa Home tuşu doğrudan Loop Home'a gider."
        warn "Acil durumda geri açmak için: adb shell pm enable com.google.android.apps.tv.launcherx"
        if ask "Google TV ana ekranını (launcherx) kapat?" E; then
            tv pm disable-user --user 0 com.google.android.apps.tv.launcherx >/dev/null
            done_step "Google TV ana ekranı kapatıldı" "pm enable com.google.android.apps.tv.launcherx"
        fi
    fi

    bold "Fotoğraflar ve gece modu"
    say "Arka plan fotoğrafları NASA'dan gelir. Unsplash fotoğrafları (doğa, mimari, hayvanlar…) için"
    say "KENDİ ücretsiz Unsplash anahtarın gerekir — başkasının anahtarı kullanılmaz:"
    say "  1. unsplash.com'da hesap aç  2. unsplash.com/oauth/applications → New Application"
    say "  3. Uygulamanın sayfasındaki 'Access Key'i kopyala (Secret key gerekmez)"
    read -r -p "Unsplash Access Key (boş bırak = sadece NASA): " UKEY </dev/tty
    say "Gece modu güneşin batışına göre ekranı karartır. Şehrinin konumu, örnekler:"
    say "  İstanbul 41.01,28.98 · Ankara 39.93,32.86 · İzmir 38.42,27.14 · Bursa 40.19,29.06 · Antalya 36.90,30.71"
    read -r -p "Enlem,boylam (boş bırak = Ankara): " LOC </dev/tty
    ARGS=(-n "$LOOPHOME/.MainActivity" -a "$LOOPHOME.CONFIG")
    [ -n "$UKEY" ] && ARGS+=(--es unsplash_key "$UKEY")
    if [[ "$LOC" =~ ^[[:space:]]*(-?[0-9.]+)[[:space:]]*,[[:space:]]*(-?[0-9.]+)[[:space:]]*$ ]]; then
        ARGS+=(--es lat "${BASH_REMATCH[1]}" --es lon "${BASH_REMATCH[2]}")
    fi
    if [ ${#ARGS[@]} -gt 4 ]; then
        if [ "$DRY" = 1 ]; then
            say "  [deneme] am start … CONFIG (anahtar ve konum gönderilir)"
        else
            adb -s "$TV" shell am start "${ARGS[@]}" </dev/null >/dev/null
        fi
        done_step "Loop Home'a Unsplash anahtarı$([ -n "$LOC" ] && echo " ve konum") gönderildi (anahtar bu kayda yazılmaz)"
    fi
    tv am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
fi

# ---------------------------------------------------------------- 4. uygulamalar
bold "4) Uygulamalar"
say "Her birini ayrı ayrı soracağım."

# GitHub'daki son sürümden bu kutunun işlemcisine uygun APK'nın adresini yazar.
github_apk() {
    curl -fsSL "https://api.github.com/repos/$1/releases/latest" | python3 -I -c '
import json, sys
abi = sys.argv[1]
names = {"armeabi-v7a": ["armeabi-v7a", "arm32v7", "armv7", "arm32"],
         "arm64-v8a": ["arm64-v8a", "arm64v8", "arm64", "aarch64"]}[abi]
apks = [a for a in json.load(sys.stdin).get("assets", []) if a["name"].endswith(".apk")
        and not any(x in a["name"].lower() for x in ("x86", "x64", "google-play"))]
pick = [a for a in apks if any(n in a["name"].lower() for n in names)] \
    or [a for a in apks if "universal" in a["name"].lower()] or (apks if len(apks) == 1 else [])
print(pick[0]["browser_download_url"] if pick else "")
' "$ABI"
}

install_github() { # repo paket ad
    local url
    url="$(github_apk "$1")"
    if [ -z "$url" ]; then
        warn "$3: $1 sürümlerinde bu işlemciye ($ABI) uygun APK bulunamadı."
        return 1
    fi
    say "  indiriliyor: $url"
    if [ "$DRY" = 1 ]; then
        say "  [deneme] adb install $(basename "$url")"
        return 0
    fi
    curl -fsSL -o "$TMP/app.apk" "$url" && adb -s "$TV" install -r "$TMP/app.apk" </dev/null | tail -1
}

while IFS='|' read -r src id pkg name desc; do
    src="$(trim "$src")"; id="$(trim "$id")"; pkg="$(trim "$pkg")"
    name="$(trim "$name")"; desc="$(trim "$desc")"
    [ -z "$src" ] || [[ "$src" == \#* ]] && continue
    if has_pkg "$pkg"; then
        say "  $name zaten kurulu."
        continue
    fi
    ask "$name — $desc. Kurulsun mu?" H || continue
    case "$src" in
        github)
            install_github "$id" "$pkg" "$name" && done_step "$name kuruldu ($id)" "pm uninstall $pkg"
            ;;
        osplay)
            if [ "$ABI" = arm64-v8a ] && [ "$SDK" -ge 31 ]; then
                install_github "$id" "$pkg" "$name" && done_step "$name kuruldu ($id)" "pm uninstall $pkg"
            else
                warn "Osplay'in resmi APK'sı 64-bit ve Android 12+ istiyor; bu kutu $ABI / API $SDK."
                say  "  Kaynaktan bu kutuya uygun derlemek için: tools/osplay-32bit.sh (Android SDK + NDK gerekir)."
                say  "  Derledikten sonra: adb -s $TV install -r <çıkan APK>"
            fi
            ;;
        play)
            tv am start -a android.intent.action.VIEW -d "market://details?id=$pkg" >/dev/null
            read -r -p "  TV'de Play Store açıldı: 'Yükle'ye bas, bitince Enter… " _ </dev/tty
            done_step "$name Play Store'dan kuruldu" "pm uninstall $pkg"
            ;;
    esac
    if [ "$pkg" = org.smarttube.stable ] && has_pkg com.google.android.youtube.tv; then
        if ask "  Resmi YouTube'un arka planda çalışmasını (öneri servisi) kısıtla? Açınca yine çalışır." E; then
            tv cmd appops set com.google.android.youtube.tv RUN_ANY_IN_BACKGROUND ignore
            tv cmd appops set com.google.android.youtube.tv RUN_IN_BACKGROUND ignore
            done_step "YouTube arka plan çalışması kısıtlandı" "cmd appops set com.google.android.youtube.tv RUN_ANY_IN_BACKGROUND allow; cmd appops set com.google.android.youtube.tv RUN_IN_BACKGROUND allow"
        fi
    fi
done < "$HERE/apps.txt"

bold "Bitti."
say "Yapılanlar ve geri alma komutları: $LOG"
say "Loop Home ipuçları: TV+ basılı tut = gizli menü (Loop Home ayarları, uyku zamanlayıcısı),"
say "Home basılı tut = açık uygulamalar, ↑↑↓↓ = gizli menü. Ayrıntılar: README.md"
