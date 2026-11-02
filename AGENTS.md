# AGENTS.md — yapay zekâ asistanları için proje rehberi

Bu dosya, bu depoyla çalışan bir yapay zekâ asistanının (Claude Code, Codex, Cursor…) kutuya
ne yapıldığını, neden yapıldığını ve nerede tuzak olduğunu tek seferde anlaması için yazıldı.
İnsanlar için kısa anlatım: [README.md](README.md). Kutudaki her değişikliğin tam listesi ve geri
alma komutları: [docs/SISTEM-DEGISIKLIKLERI.md](docs/SISTEM-DEGISIKLIKLERI.md).

## Ne bu?

1. **Loop Home** (`loophome/`): Google TV kutusu için yazılmış, Gradle'sız, saf Java bir ana ekran
   (launcher). Paket `com.mustycetin.loophome`. 3x3 sonsuz döngülü cam grid, NASA/Unsplash arka plan
   fotoğrafları, haber bandı, piyasa kartı, podcast oynatıcı, uyku zamanlayıcısı, açık uygulamalar
   ekranı, tüm uygulamalar listesi.
2. **Kurulum** (`setup/setup.sh`): kutuyu adb ile temizleyen, ayarlayan, Loop Home'u ve seçilen
   uygulamaları kuran etkileşimli betik. Her değişikliği geri alma komutuyla kaydeder.
3. **Araçlar** (`tools/`): ör. Osplay'i 32-bit kutular için derleme tarifi.

## Hedef cihaz (önemli varsayımlar)

| | |
|---|---|
| Cihaz | Xiaomi TV Box S 2. nesil — model `MiTV-AFKR0`, kod adı `jaws` |
| Sistem | Google TV, Android 11 (API 30) |
| İşlemci ABI | **sadece 32-bit** `armeabi-v7a` (çekirdek 64-bit olsa da kullanıcı alanı 32-bit) |
| RAM | 2 GB — bellek dar; Loop Home ~170–200 MB PSS kullanır |
| Kumanda | Bluetooth, `Vendor_2717_Product_32b9.kl` |

- Sadece arm64 için derlenmiş APK'lar `INSTALL_FAILED_NO_MATCHING_ABIS` ile kurulmaz. GitHub'dan
  indirirken `armeabi-v7a` / `arm32v7` / `universal` olanı seç (setup.sh bunu otomatik yapar).
- minSdk 31+ isteyen uygulamalar kurulmaz; kaynaktan minSdk 30 ile derlemek çoğu zaman çalışır
  (örnek: `tools/osplay-32bit.sh`).

## Kutuya erişim

- Kutuda geliştirici modu + USB hata ayıklama açık olmalı; bağlantı ağ üzerinden:
  `adb connect <IP>:5555`, TV ekranında izin penceresi onaylanır.
- IP DHCP ile değişebilir; kullanıcıya sor, kodda/dokümanda gerçek IP tutma.

## Kurallar (bunlara uy)

1. **Kullanıcı TV'yi kullanırken `adb shell input keyevent` gönderme.** Ayrıca adb ile enjekte edilen
   tuşlar erişilebilirlik filtresinden GEÇMEZ; Home basılı tutma, TV+ ve liste tuşu gibi
   KeyService özellikleri adb ile test edilemez — kumandayla kullanıcı test eder. Doğrulamayı
   `screencap`, `dumpsys`, `logcat` ve `am start` ile intent göndererek yap.
2. **Kurulum ana ekranı yeniden başlatır**; kurmadan önce kullanıcıya haber ver.
3. Paket kaldırırken `pm uninstall -k --user 0` (geri alınabilir) ya da `pm disable-user --user 0`
   kullan. Sistem uygulaması geri gelirse `disable-user` daha kalıcıdır. Her paketi kullanıcıya
   tek tek onaylat.
4. Loop Home'u her güncellemeden (`adb install -r`) sonra **erişilebilirlik servisini yeniden ekle**:
   Android bu kutuda güncellemede `KeyService`'i `enabled_accessibility_services` listesinden düşürüyor.
   Listedeki diğer servisleri (`:` ile ayrılmış) silme. `build.sh --install` bunu otomatik yapar.
5. Güncelleme çökmeye yol açarsa hemen önceki APK'ya dön (`adb install -r -d eski.apk`), sonra hatayı
   `adb logcat -b crash` ile incele. Kutu ana ekransız kalmasın.
6. Sırları (Unsplash anahtarı, keystore, şifre, IP) asla depoya yazma; `loophome/local/` gitignore'da.

## Bilinen tuzaklar (hepsi yaşandı)

- **partnercustomizer** (`com.xiaomi.android.tvsetup.partnercustomizer`): her açılışta Netflix,
  Prime, Disney+, Exxen, iQIYI, Digiturk'ü Play Store üzerinden zorla geri kurar. Kaldırılmazsa
  temizlik her yeniden başlatmada boşa gider.
- **setupwraith** (`com.google.android.tungsten.setupwraith`): ilk kurulumda bağlanan Wi-Fi ağı bu
  uygulamaya aittir; kaldırınca Wi-Fi bilgisi gider. Önce Ayarlar → Ağ'dan ağı unutup yeniden
  bağlan (ağ `com.android.tv.settings`'e geçer), sonra kaldır.
- **Kumandanın özel tuşları** (Netflix, Prime, YouTube vb.) Android'e `KEYCODE_BUTTON_1…16` olarak
  gelir ve Xiaomi sistemi bunları uygulamalara ulaşmadan yakalar. Bunları sadece bir
  **erişilebilirlik servisi** (`flagRequestFilterKeyEvents`) sistemden önce görebilir. Bu yüzden
  liste tuşu ataması ve TV+ tuşu `KeyService` içinde yakalanır, Activity'de değil.
  TV+ tuşu = `KEYCODE_BUTTON_5` (192). `BUTTON_6` (193) açık uygulamalar ekranını açar.
- **Stok launcher** (`com.google.android.apps.tv.launcherx`) kapatılır; Loop Home tek HOME olur.
  Acil durumda: `adb shell pm enable com.google.android.apps.tv.launcherx`.
- Resmi YouTube'un öneri servisi ayrı paket değil (`com.google.android.youtube.tv` içinde bir
  işlem); `cmd appops set … RUN_ANY_IN_BACKGROUND ignore` ile kısıtlanır.
- Hesap tabanlı öneriler (YouTube/Prime satırları) root'suz bir launcher'dan okunamaz
  (TvProvider boş döner, başka paketlerin satırları gizli) — denendi, uğraşma.

## Loop Home kodu hakkında

- Kaynak, kaybolan orijinalden **jadx ile APK'dan geri çıkarıldı** (bkz. docs/GELISTIRME.md).
  Yerel değişken adları (`r4`, `str2`, `f13`…) jadx'ten geliyor, alan/metot adları orijinal.
  Lambda gövdeleri `lambda_<metot>_<n>` adını taşır. Okunaklılık için yeniden adlandırmak serbest,
  ama davranışı değiştirmeden ve her adımda derleyip test ederek.
- Bilinen jadx tuzağı: alan başlatma sırası. `PhotoFeed.unsplashKey` alan tanımında
  okunuyordu, `ctx` atanmadan → açılışta çökme. Kurucuya taşındı. Benzer bir şey görürsen smali ile
  (`apktool d`) karşılaştır.
- Derleme: `loophome/build.sh` (aapt2 → javac → d8 → zipalign → apksigner). Gradle yok.
- Görsel tercih: **sade**. Kenar önizlemesi/ikon zıplaması/paralaks denendi, kalabalık ve daha az akıcı
  bulundu; şimdi "Loop Home ayarları"nda varsayılan kapalı bir seçenek. Yeni görsel efekt
  eklerken bellek (2 GB) ve akıcılığı ölç; sürekli çalışan animasyon ekleme.
- Ayarlar (`SharedPreferences "settings"`): `peek` (efektler), `list_key` (liste tuşu kodu),
  `unsplash_key`, `lat`/`lon` (gece modu konumu). Unsplash anahtarı ve konum, setup.sh tarafından
  `am start -n com.mustycetin.loophome/.MainActivity -a com.mustycetin.loophome.CONFIG
  --es unsplash_key … --es lat … --es lon …` ile gönderilir.

## Kumanda ve tuşlar (Loop Home)

| Tuş | İşlev |
|---|---|
| Yön tuşları / OK | grid'de gez / uygulamayı aç; OK basılı tut = uygulama bilgisi |
| Home | ana ekran; **Home basılı tut** = açık uygulamalar (KeyService) |
| TV+ (BUTTON_5) | kısa bas = açık uygulamalar; **basılı tut** = gizli menü (KeyService) |
| ↑ ↑ ↓ ↓ | gizli menü |
| Atanan liste tuşu | her yerden tüm uygulamalar listesini aç/kapat (KeyService) |
| Gizli menü | uyku zamanlayıcısı, şimdi kapat, sonraki fotoğraf, Android ayarları, ses, Loop Home ayarları |

## Dış veri kaynakları

Unsplash (kullanıcının kendi anahtarıyla), Wikimedia Commons / NASA (uzay fotoğrafları),
NASA podcast akışı, BBC Learning English (6 Minute English), kantan.news (haber bandı ve
podcastler), finans.truncgil.com (döviz/altın kartı). Hepsi anahtarsız çalışır, Unsplash hariç.
