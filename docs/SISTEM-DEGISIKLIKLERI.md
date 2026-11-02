# Kutuda yapılan sistem değişiklikleri

Referans kutu: Xiaomi TV Box S 2. nesil (MiTV-AFKR0, Google TV 11). Bu liste, çalışan bir kutudan
`pm list packages -u/-d`, `settings get` ve `appops get` ile okunarak çıkarıldı; `setup/setup.sh`
aynı adımları seçmeli olarak uygular. Bütün komutlar `adb -s <IP>:5555 shell …` ile çalışır.

Hepsi geri alınabilir. `pm uninstall -k --user 0` paketi cihazdan silmez, sadece bu kullanıcı için
kaldırır; fabrika ayarına dönünce de geri gelir.

## 1. Kaldırılan paketler (`pm uninstall -k --user 0 <paket>`)

Geri al: `cmd package install-existing <paket>`

| Paket | Ne | Neden |
|---|---|---|
| `com.xiaomi.android.tvsetup.partnercustomizer` | Xiaomi iş ortağı özelleştirici | **Tuzak:** her açılışta Netflix, Prime, Disney+, Exxen, iQIYI, Digiturk'ü Play'den zorla kurar |
| `android.autoinstalls.config.xioami.mibox3` | Otomatik kurulum ayarı | uygulama dayatma |
| `com.miui.tv.analytics` | Xiaomi analitik | telemetri |
| `com.xiaomi.statistic` | Xiaomi istatistik | telemetri |
| `com.google.android.feedback` | Google geri bildirim | gereksiz |
| `com.google.android.partnersetup` | Google iş ortağı kurulumu | gereksiz |
| `com.google.android.play.games` | Play Games | gereksiz |
| `com.google.android.youtube.tvmusic` | YouTube Music | gereksiz (isteyen tutabilir) |
| `com.android.printspooler` | Yazdırma | TV'de kullanılmaz |
| `com.google.android.apps.tv.dreamx` | Google TV ekran koruyucusu (Ambient) | Loop Home'un kendi fotoğrafları var; RAM |
| `com.mitv.gallery` | Xiaomi galeri | gereksiz |
| `com.mitv.videoplayer` | Xiaomi video oynatıcı | gereksiz |
| `com.xiaomi.mimusic2` | Xiaomi müzik | gereksiz |
| `com.xiaomi.mitv.mediaexplorer` | Xiaomi medya gezgini | gereksiz |
| `com.xm.webcontent` | Xiaomi web içerik | gereksiz |
| `com.mitv.tvhome.atv` | Xiaomi ana ekran eklentileri | stok ana ekran kullanılmıyor |
| `com.mitv.tvhome.michannel` | Xiaomi kanal önerileri | reklam/öneri |
| `com.mitv.tvhome.mitvplus` | Xiaomi TV+ önerileri | reklam/öneri |
| `com.mitv.tvhome.oemtab` | Xiaomi ana ekran sekmesi | stok ana ekran kullanılmıyor |
| `com.google.android.tungsten.setupwraith` | İlk kurulum sihirbazı | **Tuzak:** Wi-Fi ağı ona aitse Wi-Fi gider. Önce ağı Ayarlar'dan unutup yeniden bağla |

## 2. Kapatılan paketler (`pm disable-user --user 0 <paket>`)

Geri al: `pm enable <paket>`

| Paket | Ne | Not |
|---|---|---|
| `com.google.android.apps.tv.launcherx` | Google TV ana ekranı | Loop Home tek ana ekran olur. **Acil durum çıkışı:** `pm enable` |
| `com.netflix.ninja` | Hazır kurulu Netflix | kullanılmıyordu |
| `com.amazon.amazonvideo.livingroom` | Hazır kurulu Prime Video | kullanılmıyordu |
| `com.xiaomi.mitv.updateservice` | Xiaomi OTA güncellemeleri | güncellemeler temizliği geri alabilir; isteğe bağlı |

## 3. Sistem ayarları

| Komut | Geri al | Neden |
|---|---|---|
| `settings put global private_dns_mode hostname`<br>`settings put global private_dns_specifier dns.adguard-dns.com` | `settings put global private_dns_mode opportunistic` | AdGuard DNS: reklam ve izleyici engelleme |
| `settings put global window_animation_scale 0.5`<br>(+ `transition_animation_scale`, `animator_duration_scale`) | aynı ayarlar `1` | arayüz daha çevik |
| `settings put secure screensaver_enabled 0` | `… 1` | dreamx kaldırıldı; Loop Home'un uyku zamanlayıcısı var |

## 4. Loop Home izinleri ve tuş yakalama

| Komut | Neden |
|---|---|
| `appops set com.mustycetin.loophome GET_USAGE_STATS allow` | "Açık uygulamalar" ekranı son kullanılan uygulamaları okur |
| `settings put secure enabled_accessibility_services com.mustycetin.loophome/com.mustycetin.loophome.KeyService[:diğerleri]`<br>`settings put secure accessibility_enabled 1` | `KeyService` erişilebilirlik servisi tuşları sistemden önce görür |

`KeyService`'in yakaladığı tuşlar:

| Tuş | Davranış | Neden erişilebilirlik? |
|---|---|---|
| Home (3) | kısa = ana ekran (`GLOBAL_ACTION_HOME`), basılı tut (550 ms) = açık uygulamalar | Home'u uygulamalar göremez |
| TV+ (`BUTTON_5`, 192) | kısa = açık uygulamalar, basılı tut (600 ms) = gizli menü | Xiaomi bu tuşu uygulamalara ulaşmadan yakalar |
| Atanan liste tuşu (genelde bir `BUTTON_x`) | her yerden tüm uygulamalar listesini aç/kapat | aynı sebep |
| OK (uyku uyarısı ekrandayken) | uyku zamanlayıcısına +15 dk | uyarı başka uygulamanın üstünde |

Uyku zamanlayıcısı/“şimdi kapat”, cihazı `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` ile uyutur.

**Bilinen davranış:** Loop Home her güncellendiğinde (`adb install -r`) bu kutu `KeyService`'i
erişilebilirlik listesinden düşürür; yeniden eklemek gerekir (`loophome/build.sh --install` yapar).

## 5. Uygulama kısıtları

| Komut | Geri al | Neden |
|---|---|---|
| `cmd appops set com.google.android.youtube.tv RUN_ANY_IN_BACKGROUND ignore`<br>`cmd appops set com.google.android.youtube.tv RUN_IN_BACKGROUND ignore` | aynı komutlar `allow` | resmi YouTube'un arka planda çalışan öneri işlemi RAM yiyordu (SmartTube kullanılıyor); YouTube açılınca yine çalışır |

## Durumu kontrol etme

```bash
adb shell pm list packages -d                                   # kapatılanlar
adb shell 'pm list packages -u | sort > /data/local/tmp/a; pm list packages | sort > /data/local/tmp/b; comm -23 /data/local/tmp/a /data/local/tmp/b'   # kaldırılanlar
adb shell settings get secure enabled_accessibility_services
adb shell cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN
adb shell dumpsys meminfo com.mustycetin.loophome | grep 'TOTAL PSS'
```
