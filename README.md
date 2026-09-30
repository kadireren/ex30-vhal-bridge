# EX30 VHAL Bridge

Volvo EX30 araç ünitesindeki izin verilen VHAL property'lerini CrowPanel
ESP32-S3 veya iPhone dashboard'a öncelikle BLE, isteğe bağlı yedek olarak yerel
UDP ile gönderen küçük Android Automotive (AAOS) uygulaması.

- Açılınca yayın servisini otomatik başlatır.
- İstemcinin BLE abonelik karakteristiğinde seçtiği property'leri okur.
- Seçilmeyen property için callback kaydetmez.
- Değişiklikleri en fazla yaklaşık 30 paket/saniye hızında birleştirir.
- Sabit IP kullanmaz; UDP aboneliğinin geldiği adrese cevap verir.

Bu proje salt okunur köprüdür: VHAL'e yazmaz, OBD/UDS konuşmaz, internete
telemetri göndermez.

## Mimari

```text
EX30 AAOS VHAL ──► ex30-vhal-bridge ──BLE GATT──► CrowPanel / iPhone
                              └──UDP fallback──► aynı yerel ağ istemcisi
```

İlgili açık kaynak bileşenler:

- [ex30-sensor-lab](https://github.com/kadireren/ex30-sensor-lab) — VHAL/OBD doğrulama
- [ex30-ios-dashboard](https://github.com/kadireren/ex30-ios-dashboard) — iPhone gösterge

CrowPanel firmware ayrı repodadır; bu köprü ile ortak BLE UUID ve paket
sürümünü paylaşır.

## BLE protokolü

| Alan | Değer |
|------|--------|
| Servis UUID | `7d2f0001-8d3b-4a6c-9f21-6a9b4e303001` |
| Telemetri UUID | `7d2f0002-8d3b-4a6c-9f21-6a9b4e303001` |
| Abonelik UUID | `7d2f0003-8d3b-4a6c-9f21-6a9b4e303001` |
| Protokol sürümü | `1` |

Paketler küçük binary GATT yazımlarıdır (`write without response`). Araçtaki
hücresel internet yönlendirmesi etkilenmez.

## UDP fallback (yedek)

| Yön | Port |
|-----|------|
| CrowPanel → bridge abonelik | UDP `4211` |
| Bridge → CrowPanel telemetri | UDP `4210` |

Yalnız yerel ağ içindir; sabit IP yoktur.

## VHAL anahtarları

`speed`, `perf_speed`, `power`, `soc`, `range`, `gear`, `current_gear`,
`ignition`, `parking_brake`, `outside_temp`, `night_mode`, `charge_port`,
`battery_energy`, `battery_capacity` — tanımlar `VhalCatalog.kt` içindedir.

## Derleme

Gereksinimler: JDK 17+, Android SDK 35, Gradle Wrapper (repoda).

```sh
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
./gradlew testDebugUnitTest lintDebug assembleDebug
```

İmzalı release AAB için:

1. `keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayın.
2. `*.jks` / `*.keystore` dosyasını repo dışından getirip `storeFile` yolunu yazın.
3. `./gradlew bundleRelease`

`local.properties`, `keystore.properties` ve `*.jks` Git'e girmez.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Hazır çıktılar (isteğe bağlı):

- `releases/ex30-vhal-bridge-debug.apk`
- `releases/ex30-vhal-bridge-release.aab`
- `releases/SHA256SUMS`

## Güvenlik ve gizlilik

Özet: [docs/SECURITY.md](docs/SECURITY.md)

- Repo'da API anahtarı, şifre, Play imza private key'i yoktur.
- Araç VIN'i, konum geçmişi veya kişisel sürüş kaydı tutulmaz / iletilmez.
- BLE/UDP yalnız abone olunan sensör sayılarını taşır.

## Durum

Uygulama planı ve doğrulama listesi: [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md)

## Lisans

[MIT](LICENSE)
