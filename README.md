# Şarj Metre

Telefonun bataryasına anlık olarak giren ya da çıkan akımı (mA), Internet Speed Meter'ın hızı gösterdiği gibi **status bar'da** gösteren Android uygulaması.

## Özellikler

- Status bar'da saniyede bir güncellenen canlı akım ikonu: şarjda `1850 mA`, kullanımda `-420 mA`. İstenirse watt olarak da gösterilebilir.
- Bildirimde güç (W), voltaj, sıcaklık ve kalan süre (tam dolmasına ya da bitmesine).
- Günün toplamları: bugün kaç mAh / yüzde şarj edildi, kaç mAh / yüzde harcandı.
- Akımın birimi (µA/mA) ve yönü cihaza göre otomatik tespit edilir.
- Samsung'da kalan süre, kilit ekranındaki tahminle aynıdır. Pil koruma (%80/85/…) sınırı ayarlanabilir.
- Ekran kapalıyken güncelleme durur; telefon açılınca gösterge kendiliğinden başlar.

## Derleme

Gereken: JDK 17+ (Android Studio'nun JBR'ı yeterli) ve Android SDK (platform 37).

```
gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
gradlew testDebugUnitTest      # birim testleri
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Telefonda: bildirim iznini ver, uygulamanın pil kullanımını **Kısıtlanmamış** yap. Samsung'da ayrıca Pil → Arka plan kullanım sınırları → **Hiç uyumayan uygulamalar** listesine ekle.

## Teknik

Kotlin, Jetpack Compose, minSdk 26. Ön plan servisi (`specialUse`), `BatteryManager` özellikleri ve `ACTION_BATTERY_CHANGED` yayınıyla çalışır; root gerekmez.

Barlow yazı tipi SIL Open Font License ile dağıtılır (`third_party/fonts/OFL.txt`).
