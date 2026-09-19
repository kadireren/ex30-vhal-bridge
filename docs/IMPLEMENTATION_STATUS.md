# Uygulama planı ve mevcut durum

Son güncelleme: 2026-09-19

Bu repo, `ex30-crowpanel-dashboard` projesinin ikinci aşamasıdır. Uygulama araç
ünitesinde açıldığı anda foreground service başlatır, CrowPanel aboneliğini UDP
4211'de dinler ve seçilen VHAL değerlerini UDP 4210'a yollar.

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

## Doğrulananlar

- `testDebugUnitTest`, `lintDebug`, `assembleDebug` ve `bundleRelease` başarılı.
- Debug APK ve imzalı AAB checksum'ları `releases/SHA256SUMS` içindedir.
- Derleme JDK 21 üzerinde Java/Kotlin 17 hedefiyle ve Android SDK 35 ile geçti.

## Fiziksel doğrulama bekleyen işler

- APK'yı gerçek EX30 ünitesine kurup uygulamanın VHAL izinlerini doğrula.
- Araç Wi-Fi ayarlarından `EX30-CrowPanel` erişim noktasına bir kez elle bağlan.
- Araç internetsiz SoftAP'te kalmıyorsa telefon hotspot'u, gerekirse fiziksel
  mini router ile aynı UDP protokolünü test et.
- Seçilen sensör listesi değişince callback kayıtlarının gerçekten daraldığını
  ve panelde veri akışının kesintisiz güncellendiğini doğrula.
- Hız, güç, SOC, menzil ve `NIGHT_MODE` değerlerini Sensor Lab sonuçlarıyla
  karşılaştır.
- CrowPanel bağlantısı kesildiğinde servisin bekleme durumuna geçtiğini ve geri
  geldiğinde elle yeniden başlatmadan yayın yaptığını doğrula.
- En az iki saatlik sürüş/park döngüsünde foreground service ve UDP kararlılığını
  ölç.

## Yeni bilgisayarda devam

1. Repoyu klonla.
2. `local.properties` içine o bilgisayarın Android SDK yolunu yaz.
3. `./gradlew testDebugUnitTest lintDebug assembleDebug` çalıştır.
4. Hazır araç kurulumu için `releases/ex30-vhal-bridge-debug.apk` dosyasını
   kullan.
5. Yeni imzalı AAB üretilecekse özel keystore'u GitHub dışında güvenli yoldan
   getir ve `keystore.properties` yolunu yerel olarak yapılandır.
