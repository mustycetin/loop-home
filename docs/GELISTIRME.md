# Loop Home geliştirme

## Derleme

```bash
cd loophome
./build.sh --check                     # sadece derle, imzasız
./build.sh                             # derle + imzala → build/LoopHome.apk
TV=192.168.1.50:5555 ./build.sh --install   # + TV'ye kur, erişilebilirlik servisini geri ekle
TV=192.168.1.50:5555 ./build.sh --home      # + ana ekranı yeniden başlat
```

Gerekenler: Android SDK (build-tools, `platforms/android-34`), JDK 17, adb. Gradle yok.

Gizli dosyalar `loophome/local/` içinde durur (git'e girmez; başka yer için `LOOPHOME_SECRETS=`):

| Dosya | |
|---|---|
| `loophome.keystore` | İmza anahtarı. Yoksa ilk derlemede oluşturulur. **Yedekle**: kaybolursa güncelleme kurulamaz, uygulamayı silip baştan kurmak (ve izinleri yeniden vermek) gerekir. |
| `keystore.pass` | Keystore şifresi. Yoksa rastgele üretilir. |
| `keys.properties` | İsteğe bağlı `unsplash_key=…`; APK'ya gömülür. Tercih edilen yol setup.sh ile cihaza göndermek. |
| `tv.conf` | İsteğe bağlı `TV=192.168.x.y:5555` |

Başkasının imzaladığı bir Loop Home kuruluysa, kendi derlediğini üstüne kuramazsın
(`INSTALL_FAILED_UPDATE_INCOMPATIBLE`): önce `adb uninstall com.mustycetin.loophome`, sonra
setup.sh'nin 3. adımı (izinler) yeniden.

## Yapı

```
loophome/
  AndroidManifest.xml   minSdk/targetSdk 30, HOME activity + KeyService
  build.sh
  src/com/mustycetin/loophome/
    MainActivity.java   ana ekran: düzen, saat, fotoğraf geçişleri, gizli menü, Loop Home ayarları,
                        tuş atama, podcast oynatıcı, uyku/karartma, gece modu, karşılama
    LoopGrid.java       3x3 sonsuz döngülü cam grid (Canvas ile çizim, animasyonlar)
    AppListView.java    tüm uygulamalar listesi
    RecentsView.java    açık uygulamalar / podcast rafı (yatay kartlar)
    KeyService.java     erişilebilirlik servisi: Home/TV+ basılı tutma, liste tuşu, uyku rozeti
    PhotoFeed.java      arka plan fotoğrafları (Unsplash + Wikimedia/NASA), disk önbelleği
    NewsTicker.java     haber bandı (kantan.news)
    FinanceCard.java    döviz/altın kartı, Finance.java veriyi çeker (truncgil)
    Podcasts.java       podcast rafının listesi
    Sun.java            gün doğumu/batımı → gece modu (konum: setup.sh, varsayılan Ankara)
    Net.java            HTTP yardımcıları
  res/                  string, tema, erişilebilirlik servis tanımı
  assets/fonts          Outfit (SIL OFL 1.1)
  assets/icons          Delta Icons (CC BY-NC-ND 4.0) — bazı uygulamalar için özel ikonlar
```

## Kaynağın geçmişi

Orijinal kaynak klasörü kayboldu; kutuda kurulu APK'dan **jadx 1.5** ile geri çıkarıldı
(`jadx --no-res`, kaynaklar `apktool d -s` ile). Kod küçültülmeden derlendiği için alan/metot
adları orijinal, yerel değişken adları jadx'in. Yapılan elle düzeltmeler:

- jadx'in `$$ExternalSyntheticLambda` sarmalayıcıları gerçek Java lambda'larına çevrildi
  (`lambda_<metot>_<n>`).
- `PhotoFeed`: fotoğraf döngüsünün try/finally yapısı yeniden yazıldı; `unsplashKey` alan
  başlatıcısından kurucuya taşındı (yoksa `ctx` null → açılışta çökme).
- `MainActivity.play`: final parametreye atama; iki `LinkedHashMap` tipi; dialog tema sabiti
  (`Theme_DeviceDefault_Dialog_Alert`).

Doğrulama yöntemi: yeniden derlenen APK'nın sınıf/metot listesi `dexdump` ile, kurucuların
alan atama sırası `apktool` smali'siyle orijinalle karşılaştırıldı.

## Test

- `./build.sh --check` derlemeyi doğrular.
- Kutuda: kur → `adb logcat -b crash` boş mu → `adb exec-out screencap -p > ekran.png` →
  `dumpsys accessibility` iki servis de bağlı mı → `dumpsys meminfo com.mustycetin.loophome`.
- Tuş özellikleri (Home/TV+ basılı tutma, liste tuşu) sadece gerçek kumandayla test edilebilir.
- Liste/ayar ekranlarını tuşsuz açmak için intent: `am start -n com.mustycetin.loophome/.MainActivity
  -a com.mustycetin.loophome.LIST` (açık/kapalı arasında geçiş yapar).
