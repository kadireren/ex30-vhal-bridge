# Uygulama planı ve mevcut durum

Son güncelleme: 2026-09-30

AAOS üzerinde çalışan VHAL → BLE (UDP yedek) köprüsü. Uygulama açıldığında
foreground service başlar, CrowPanel veya iPhone dashboard'a BLE ile bağlanır ve
seçilen VHAL değerlerini binary GATT paketleriyle yollar.

## Tamamlanan yazılım

- `com.kadireren.ex30vhalbridge` bağımsız AAOS uygulaması.
- Abonelikteki sensörlere göre yalnız gerekli `CarPropertyManager` callback'leri.
- Hız 10 Hz, güç en fazla 30 Hz; diğerleri property sınırına göre; statikler
  `ONCHANGE`.
- UDP mesajlarında protokol sürümü, oturum, sıra, zaman ve sensör değerleri;
  birleştirme ~30 paket/s.
- CrowPanel / dashboard adresi gelen abonelikten öğrenilir; sabit IP yok.
- Foreground service, başka ekrana geçilse de sürer.
- Debug APK ve imzalı release AAB `releases/` altında (checksum: `SHA256SUMS`).
- BLE: özel servis UUID, abonelik read/notify, telemetri write-without-response.
- BLE paketi: sürüm, sıra, monotonik zaman, anahtar/değer; panel eski/tekrarlı
  paketleri reddeder.

## Doğrulananlar

- `testDebugUnitTest`, `lintDebug`, `assembleDebug` ve `bundleRelease` başarılı.
- Derleme JDK 21 üzerinde Java/Kotlin 17 hedefiyle, Android SDK 35.

## Fiziksel doğrulama bekleyen işler

- Gerçek EX30 ünitesinde VHAL izinleri ve kurulum.
- BLE izinleri ile CrowPanel / iPhone bağlantısının Wi-Fi değiştirmeden kurulması.
- Sensör listesi değişince callback daralması ve kesintisiz panel güncellemesi.
- Hız, güç, SOC, menzil, `NIGHT_MODE` — Sensor Lab ile karşılaştırma.
- Bağlantı kopunca bekleme; geri gelince elle restart olmadan yayın.
- Vgate istemci ile VHAL sunucu aynı anda açıkken uzun sürüş/park döngüsünde
  BLE gecikme / kopma / yeniden bağlanma; UDP fallback'i bir kez.

## Yeni bilgisayarda devam

1. Repoyu klonla.
2. `local.properties` içine Android SDK yolunu yaz (`sdk.dir=...`).
3. `./gradlew testDebugUnitTest lintDebug assembleDebug`
4. Araç kurulumu için `releases/ex30-vhal-bridge-debug.apk` veya yeni debug build.
5. Yeni imzalı AAB: `keystore.properties.example` → `keystore.properties`, JKS'yi
   Git dışında tut, `./gradlew bundleRelease`.

Güvenlik / gizlilik kontrol listesi: [SECURITY.md](SECURITY.md)
