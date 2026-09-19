# EX30 VHAL Bridge

EX30 araç ünitesindeki izin verilen VHAL property'lerini CrowPanel ESP32-S3'e
öncelikle BLE, isteğe bağlı yedek olarak yerel UDP ile gönderen küçük AAOS uygulaması.

- Açılınca yayın servisini otomatik başlatır.
- CrowPanel'in BLE abonelik karakteristiğinde seçtiği property'leri okur.
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

Normal kullanımda araç ünitesi Wi-Fi ağı değiştirmeden CrowPanel'in özel BLE
servisine bağlanır. Telemetri küçük binary paketlerle `write without response`
üzerinden gönderilir; böylece araçtaki hücresel internet yönlendirmesi etkilenmez.

Eski Wi-Fi/UDP yolu geri dönüş seçeneği olarak korunmuştur. Kullanılırsa
CrowPanel UDP 4211'e abonelik yayını gönderir ve uygulama UDP 4210'a telemetri
yollar.

Uygulanan plan ve sıradaki araç testleri: `docs/IMPLEMENTATION_STATUS.md`.
