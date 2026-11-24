# TeTi

Android telefon, tablet ve Android TV için hızlı, reklamsız IPTV oynatıcısı.

TeTi **içerik sağlamaz**. Uygulama boş gelir; kullanıcı kendi yasal yayın
sağlayıcısının Xtream Codes hesabını, M3U bağlantısını veya yerel M3U dosyasını ekler.

## Özellikler

- Xtream Codes, M3U (URL) ve yerel M3U çalma listeleri
- Canlı TV, filmler, diziler; kategori, favori, sıralama ve yıl filtresi
- EPG (XMLTV): Xtream'den otomatik, özel URL, 12/24 saat veya açılışta güncelleme, saat kaydırma
- Izgara TV rehberi (7 gün) ve sağlayıcı destekliyorsa geçmiş programları izleme (catch-up)
- Çoklu ekran (2–4 kanal aynı anda)
- Kaldığın yerden devam et, TMDB'de trend olan ve benzer içerik rafları
- TMDB'den Türkçe özet, puan, afiş, oyuncu ve yönetmen bilgisi
- Oynatıcı hareketleri: kanal çekmecesi, parlaklık/ses, çift tıkla sarma
- Ebeveyn kilidi (PIN ve kilitli kategoriler), yedekle/geri yükle, tekrar eden kanalları gizleme
- Chromecast desteği

## Derleme

```bash
./gradlew assembleDebug        # hata ayıklama APK'sı
./gradlew bundleRelease        # Google Play için AAB
./gradlew testDebugUnitTest    # birim testleri
```

TMDB anahtarı kaynak kodda tutulmaz. Yerelde `local.properties` dosyasına ya da
ortam değişkenine ekleyin (kullanıcı ayrıca Ayarlar'dan kendi anahtarını girebilir):

```properties
tmdb.apiKey=...
tmdb.readToken=...
```

CI için depo secret'ları: `TETI_TMDB_API_KEY`, `TETI_TMDB_READ_TOKEN` ve imzalama için
`TETI_KEYSTORE_BASE64`, `TETI_KEYSTORE_PASSWORD`, `TETI_KEY_ALIAS`, `TETI_KEY_PASSWORD`
(bkz. `scripts/create-release-keystore.sh`).

## Yayın belgeleri

- Gizlilik politikası: [`docs/privacy-policy.md`](docs/privacy-policy.md)
- Mağaza metinleri ve form cevapları: [`docs/store/`](docs/store/)

## Atıflar

This product uses the TMDB API but is not endorsed or certified by TMDB.
Video çözme için FFmpeg (LGPL-2.1-or-later) kullanılır; ayrıntılar uygulamadaki
Ayarlar › Hakkında ve lisanslar bölümündedir.
