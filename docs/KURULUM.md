# Kurulum rehberi

Hedef: Xiaomi TV Box S 2. nesil (diğer Google TV / Android TV 11+ kutularda Loop Home çalışır,
ama temizlik listesi bu modele göre yazıldı).

## 1. Bilgisayarda gerekenler

| | macOS | Linux |
|---|---|---|
| adb | `brew install android-platform-tools` | `sudo apt install adb` |
| python3, curl | hazır gelir | `sudo apt install python3 curl` |
| *(Loop Home'u kaynaktan derlemek için)* Android SDK + JDK 17 | Android Studio kur ya da `brew install --cask android-commandlinetools` + `sdkmanager "build-tools;36.0.0" "platforms;android-34"`, `brew install openjdk@17` | benzer |

Windows'ta WSL içinde çalıştır.

## 2. Kutuyu hazırla

1. Ayarlar → Sistem → Hakkında → **Android TV OS derlemesi**'ne 7 kez bas → geliştirici modu açılır.
2. Ayarlar → Sistem → Geliştirici seçenekleri → **USB hata ayıklama**'yı aç.
3. Ayarlar → Ağ ve İnternet → bağlı ağdan **IP adresini** not et.
4. **Wi-Fi tuzağı:** temizlikte ilk kurulum sihirbazı (setupwraith) kaldırılacaksa, önce
   Ayarlar → Ağ'dan Wi-Fi ağını **unut** ve yeniden bağlan. (Setup bunu ayrıca sorar.)

## 3. Kurulumu çalıştır

```bash
git clone <bu depo> && cd TV-LoopHome
./setup/setup.sh --dry-run    # önce dene: hiçbir şeyi değiştirmez, ne yapacağını gösterir
./setup/setup.sh              # gerçek kurulum
```

Setup sırayla sorar; her adım atlanabilir:

1. **Temizlik** — Xiaomi/Google gereksiz uygulamaları (liste: `setup/debloat.txt`). Hepsi, tek tek ya da hiç.
2. **Sistem ayarları** — Xiaomi güncellemelerini kapat, AdGuard DNS, animasyon hızı, ekran koruyucu.
3. **Loop Home** — kur, izinleri ver, Google TV ana ekranını kapat, **kendi Unsplash anahtarını**
   ve şehrini gir.
4. **Uygulamalar** — SmartTube, LocalSend, CloudStream, Osplay (GitHub'dan) ve Stremio, HBO Max,
   Apple TV, TV+, TeamViewer QuickSupport, Button Mapper (Play Store'dan), her biri ayrı sorulur.

Yaptığı her şey geri alma komutuyla `setup/kurulum-kaydi-<tarih>.txt` dosyasına yazılır.

### Unsplash anahtarı (isteğe bağlı)

Loop Home anahtarsız da çalışır: arka planda NASA/Wikimedia uzay fotoğrafları döner. Unsplash'ın
doğa, mimari, hayvan fotoğrafları için herkes **kendi** ücretsiz anahtarını kullanır:

1. unsplash.com'da hesap aç.
2. https://unsplash.com/oauth/applications → **New Application** → koşulları kabul et → ad ver.
3. Uygulama sayfasındaki **Access Key**'i kopyala (Secret key gerekmez). Ücretsiz "Demo" seviyesi
   saatte 50 istek verir; Loop Home bir istekle 30 fotoğraf alır, yeter.
4. Setup sorduğunda yapıştır. Sonradan değiştirmek için setup'ı tekrar çalıştırıp diğer adımları
   atlayabilir ya da doğrudan:
   ```bash
   adb shell am start -n com.mustycetin.loophome/.MainActivity -a com.mustycetin.loophome.CONFIG --es unsplash_key ANAHTARIN
   ```

### Konum (gece modu)

Gece modu, güneş battıktan sonra ekranı yavaşça karartır. Setup şehrinin enlem/boylamını sorar
(örn. İstanbul `41.01,28.98`); boş bırakırsan Ankara kullanılır.

## 4. Kurulumdan sonra

- **Liste tuşu:** TV+ tuşunu basılı tut → Loop Home ayarları → Uygulama listesi tuşu → kumandada
  boşta bir özel tuşa bas (ör. kullanmadığın Netflix/Prime tuşu).
- **Kenar önizlemesi ve efektler** aynı menüden açılıp kapanır (varsayılan kapalı).
- **Osplay** bu kutuda resmi APK ile kurulmaz (64-bit + Android 12 ister):
  `./tools/osplay-32bit.sh` ile kaynaktan derle, `adb install -r` ile kur.
- Kutuyu tamamen eski haline döndürmek: kurulum kaydındaki geri alma komutları, ya da fabrika ayarları.

## Sorun giderme

| Belirti | Çözüm |
|---|---|
| Home'a basınca hiçbir şey olmuyor / eski ana ekran gelmiyor | `adb shell pm enable com.google.android.apps.tv.launcherx` |
| Home basılı tutma, TV+ ya da liste tuşu çalışmıyor | Erişilebilirlik servisi düşmüş: `adb shell settings get secure enabled_accessibility_services` içinde `com.mustycetin.loophome/com.mustycetin.loophome.KeyService` olmalı; yoksa setup'ın 3. adımı ya da `build.sh --install` |
| Reboot sonrası Netflix/Prime vb. kendiliğinden geri geldi | `partnercustomizer` kaldırılmamış; temizlik adımını tekrar çalıştır |
| Wi-Fi gitti | setupwraith ağı silmiş: Ayarlar → Ağ'dan yeniden bağlan |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | Uygulamanın 32-bit (armeabi-v7a) sürümünü bul |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (Loop Home) | Farklı anahtarla imzalı sürüm kurulu: `adb uninstall com.mustycetin.loophome`, sonra yeniden kur |
