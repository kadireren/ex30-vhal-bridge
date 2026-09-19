# EX30 VHAL Bridge

EX30 araç ünitesindeki izin verilen VHAL property'lerini CrowPanel ESP32-S3'e
yerel UDP ile gönderen küçük AAOS uygulaması.

- Açılınca yayın servisini otomatik başlatır.
- CrowPanel'in abonelik mesajında seçtiği property'leri okur.
- Seçilmeyen property için callback kaydetmez.
- Değişiklikleri en fazla yaklaşık 30 paket/saniye hızında birleştirir.
- Sabit IP kullanmaz; UDP aboneliğinin geldiği adrese cevap verir.

## Derleme

```sh
printf 'sdk.dir=/Volumes/Harici/Android/sdk\n' > local.properties
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
İmzalı AAB: `app/build/outputs/bundle/release/app-release.aab`

Repoda başka bilgisayarda doğrudan kullanılabilecek hazır çıktılar da bulunur:

- `releases/ex30-vhal-bridge-debug.apk`
- `releases/ex30-vhal-bridge-release.aab`
- `releases/SHA256SUMS`

Yeni bilgisayarda kaynak derlemek için JDK 17 ve Android SDK 35 yeterlidir.
Gradle Wrapper repoya dahildir. `local.properties` makineye özgü SDK yoludur ve
yeniden oluşturulmalıdır. Özel Play imza anahtarı güvenlik nedeniyle GitHub'a
konmaz; repodaki AAB mevcut anahtarla imzalanmış hazır çıktıdır.

Uygulama CrowPanel'in `EX30-CrowPanel` ağına bağlanmış araç ünitesinde
çalıştırılır. CrowPanel UDP 4211'e abonelik yayını gönderir; uygulama UDP 4210'a
telemetri yollar.

Araç ünitesi Wi-Fi ayarlarından ağa bir kez elle bağlanmalıdır. EX30 internetsiz
erişim noktasını bırakırsa aynı protokol telefon hotspot'u veya fiziksel mini
router üzerinden değişiklik gerektirmeden çalışır.

Uygulanan plan ve sıradaki araç testleri: `docs/IMPLEMENTATION_STATUS.md`.
