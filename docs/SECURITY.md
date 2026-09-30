# Güvenlik ve gizlilik

Bu belge, repoyu herkese açık yayınlamadan önce kontrol edilen noktaları ve
katkı / kullanım sınırlarını özetler.

## Repoda olmaması gerekenler

Aşağıdakiler `.gitignore` ile dışarıda tutulur ve commit edilmemelidir:

| Dosya / desen | Neden |
|---------------|--------|
| `local.properties` | Makineye özel Android SDK yolu |
| `keystore.properties` | İmza şifreleri ve alias |
| `*.jks`, `*.keystore`, `*.p12`, `*.pem` | Özel imza / TLS anahtarları |
| `.env*`, `credentials*`, `secrets*` | Genel gizli yapılandırma |
| `.idea/`, `.gradle/`, `**/build/` | Yerel IDE / derleme artıkları |

Örnek imza şablonu: `keystore.properties.example` (gerçek şifre yok).

## Bilinçli olarak paylaşılanlar

- Kaynak kod ve birim testleri
- `releases/` altındaki debug APK ve imzalı release AAB (dağıtım kolaylığı)
- BLE servis / karakteristik UUID'leri ve UDP portları (istemci protokolü)

Release AAB'nin imza sertifikası subject alanları (ülke, şehir, CN) herkese
açık dosyadan okunabilir. Private key repoda değildir; sertifika subject'te
kişisel konum bilgisi istemiyorsanız AAB'yi kaldırın veya subject'siz yeni
anahtarla yeniden imzalayın (Play App Signing kullanıyorsanız upload key
politikasına dikkat edin).

## Çalışma zamanı davranışı

- Yalnız Android Car VHAL property'lerini **okur**; VHAL yazmaz.
- Telemetri hedefi CrowPanel / iPhone BLE (veya yerel UDP abonesi)dır.
- İnternet üzerinden telemetri, analitik veya crash raporu gönderilmez.
- VIN, plaka, hesap, konum geçmişi veya sürüş kaydı saklanmaz.
- `allowBackup=false` — uygulama yedeklemesi kapalıdır.

## Katkı güvenliği

Güvenlik açığı bildirimi için GitHub Security Advisories veya repo Issues
kullanın; exploit PoC veya araç ünitesine yetkisiz erişim adımları göndermeyin.

Şüpheli bir gizli bilginin geçmişte commit edildiğini düşünüyorsanız public
yapmadan önce geçmişi temizleyin veya anahtarı döndürün; yalnız silmek yeterli
değildir.
