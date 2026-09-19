# Uygulama planı ve mevcut durum

Son güncelleme: 2026-09-19

Bu repo, `ex30-crowpanel-dashboard` projesinin ikinci aşamasıdır. Uygulama araç
ünitesinde açıldığı anda foreground service başlatır, CrowPanel'e BLE ile bağlanır
ve seçilen VHAL değerlerini binary GATT paketleriyle yollar. UDP yolu yedektir.

## Tamamlanan yazılım

- `com.kadireren.ex30vhalbridge` bağımsız AAOS uygulaması oluşturuldu.
- CrowPanel aboneliğindeki sensörlere göre yalnız gerekli
  `CarPropertyManager` callback'leri kaydediliyor.
- Hız 10 Hz, güç en fazla 30 Hz ve diğer değerler property sınırlarına göre
  isteniyor; statik property'ler `ONCHANGE` kaydediliyor.
- UDP mesajlarında protokol sürümü, oturum, sıra numarası, zaman ve sensör
  değerleri bulunuyor. Değişiklikler yaklaşık 30 paket/saniyede birleştiriliyor.
- CrowPanel adresi gelen abonelik paketinden öğreniliyor; sabit IP gerekmiyor.
- Uygulama açıldığında servis otomatik başlıyor ve başka araç ekranına geçilse
  de foreground service olarak çalışmayı sürdürüyor.
- Debug APK ve mevcut EX30 anahtarıyla imzalanmış release AAB `releases/`
  altında saklandı.
- CrowPanel özel servis UUID'siyle taranıyor; sensör seçimi read/notify kontrol
  karakteristiğinden alınarak telemetri write-without-response karakteristiğine
  en fazla yaklaşık 30 paket/saniye hızında aktarılıyor.
- BLE paketi sürüm, sıra, monotonik zaman ve anahtar/değer çiftlerini içeriyor;
  en yeni değer öncelikli ve eski/tekrarlı paketler panelde reddediliyor.

## Doğrulananlar

- `testDebugUnitTest`, `lintDebug`, `assembleDebug` ve `bundleRelease` başarılı.
- Debug APK ve imzalı AAB checksum'ları `releases/SHA256SUMS` içindedir.
- Derleme JDK 21 üzerinde Java/Kotlin 17 hedefiyle ve Android SDK 35 ile geçti.

## Fiziksel doğrulama bekleyen işler

- APK'yı gerçek EX30 ünitesine kurup uygulamanın VHAL izinlerini doğrula.
- Araç ünitesi BLE izinlerini verip CrowPanel bağlantısının Wi-Fi değiştirmeden
  otomatik kurulmasını doğrula.
- Seçilen sensör listesi değişince callback kayıtlarının gerçekten daraldığını
  ve panelde veri akışının kesintisiz güncellendiğini doğrula.
- Hız, güç, SOC, menzil ve `NIGHT_MODE` değerlerini Sensor Lab sonuçlarıyla
  karşılaştır.
- CrowPanel bağlantısı kesildiğinde servisin bekleme durumuna geçtiğini ve geri
  geldiğinde elle yeniden başlatmadan yayın yaptığını doğrula.
- Vgate istemci bağlantısı ile VHAL sunucu bağlantısı aynı anda açıkken en az iki
  saatlik sürüş/park döngüsünde BLE gecikmesini, kopmayı ve otomatik yeniden
  bağlanmayı ölç. UDP fallback'i ayrıca bir kez doğrula.

## Yeni bilgisayarda devam

1. Repoyu klonla.
2. `local.properties` içine o bilgisayarın Android SDK yolunu yaz.
3. `./gradlew testDebugUnitTest lintDebug assembleDebug` çalıştır.
4. Hazır araç kurulumu için `releases/ex30-vhal-bridge-debug.apk` dosyasını
   kullan.
5. Yeni imzalı AAB üretilecekse özel keystore'u GitHub dışında güvenli yoldan
   getir ve `keystore.properties` yolunu yerel olarak yapılandır.
