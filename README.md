# Live Meter

Telefonun bataryasına anlık olarak giren ya da çıkan akımı (mA) ve ağ hızını, Internet Speed Meter'ın yaptığı gibi **status bar'da** gösteren Android uygulaması. Uygulamanın Batarya, Ağ ve Ayarlar olmak üzere üç sekmesi var.

## Özellikler

- Status bar'da canlı akım ikonu (yenileme aralığı ayarlanabilir): şarjda `1850 mA`, kullanımda `-420 mA`. İstenirse watt olarak da gösterilebilir.
- Bildirimde güç (W), voltaj, sıcaklık ve kalan süre (tam dolmasına ya da bitmesine).
- Günün toplamları: bugün kaç mAh / yüzde şarj edildi, kaç mAh / yüzde harcandı.
- Akımın birimi (µA/mA) ve yönü cihaza göre otomatik tespit edilir.
- Samsung'da kalan süre, kilit ekranındaki tahminle aynıdır. Pil koruma (%80/85/…) sınırı ayarlanabilir.
- Ekran kapalıyken güncelleme durur; telefon açılınca gösterge kendiliğinden başlar.
- Status bar'da ikinci ikon olarak ağ hızı (indirme + yükleme toplamı). Bildirimde indirme ve yükleme ayrı ayrı, bugünkü mobil ve Wi-Fi kullanımıyla birlikte.
- Ağ ekranı: bugün saat saat, son 7 gün ve bu ay gün gün mobil / Wi-Fi kullanımı. Veri sistemin kendi kaydından gelir (Samsung'un veri kullanımı ekranı da bunu okur), bu yüzden kurulumdan önceki günler de görünür ve VPN trafiği iki kez sayılmaz. Bunun için bir kez **Kullanım verisi erişimi** izni gerekir.
- Pil dostu: ikon yalnızca akım en az 10 mA değişince yeniden çizilir; telefon ısınınca (termal durum "orta" ve üstü) yenileme 5 saniyeye yavaşlar.

## Derleme

Gereken: JDK 17+ (Android Studio'nun JBR'ı yeterli) ve Android SDK (platform 37).

```
gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
gradlew testDebugUnitTest      # birim testleri
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release sürümü

Günlük kullanım için release sürümünü kur. R8 ile küçültülmüş ve optimize edilmiştir; ölçümde debug sürümünün yarısı kadar işlemci kullandı.

1. Anahtar repo dışında durur: `%USERPROFILE%\.android\sarjmetre-release.jks`. Şifreleri repo kökündeki `keystore.properties` dosyasındadır (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). İkisi de git'e girmez. **İkisini birlikte yedekle:** anahtar kaybolursa güncelleme için uygulamayı kaldırıp yeniden kurmak gerekir ve ayarlar sıfırlanır.
2. Derle ve kur:

   ```
   gradlew assembleRelease        # APK: app/build/outputs/apk/release/app-release.apk
   adb install -r app/build/outputs/apk/release/app-release.apk
   adb shell cmd package compile -m speed -f com.anils.sarjmetre
   ```

   Son komut uygulamayı önceden makine koduna derler (AOT). Her kurulumdan sonra tekrarla.
3. Debug sürümü kuruluysa imzalar farklı olduğu için önce `adb uninstall com.anils.sarjmetre` gerekir.

`keystore.properties` yoksa release APK imzasız çıkar.

Telefonda: bildirim iznini ver, uygulamanın pil kullanımını **Kısıtlanmamış** yap. Samsung'da ayrıca Pil → Arka plan kullanım sınırları → **Hiç uyumayan uygulamalar** listesine ekle.

## Arayüz

Arayüz, Apple'ın WWDC25'te tanıttığı Liquid Glass tasarım diline göre yapıldı:

- İçerik opak katmanda kalır. Liquid Glass yalnızca üstte yüzen sekme çubuğunda kullanılır; anahtarlar ise sadece dokunulduğu anda cama dönüşür.
- Seçili sekmenin rengi dışında renklendirme yoktur ve cam üst üste binmez.
- Köşeler sürekli eğrilidir, kontroller kapsül şeklindedir.
- Kaydırma kenarı efekti vardır. Büyük başlık kaydırınca küçülür. Bölümler gruplanmış listelerde durur.
- Renkler iOS sistem renkleridir.
- Samsung'un "şeffaflığı azalt" ayarı açıksa cam daha mat hale gelir.

## Teknik

Kotlin, Jetpack Compose, minSdk 26. Ön plan servisi (`specialUse`), `BatteryManager` özellikleri ve `ACTION_BATTERY_CHANGED` yayınıyla çalışır; root gerekmez.

## Üçüncü taraf

- Cam efekti: [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (`backdrop`, `shapes`), Apache License 2.0. `ui/glass` altındaki sekme çubuğu, anahtar ve etkileşim yardımcıları bu projenin örnek uygulamasından uyarlandı (`third_party/AndroidLiquidGlass/LICENSE`).
- Yazı tipi: Inter, SIL Open Font License (`third_party/fonts/OFL.txt`). Apple'ın SF Pro fontu yalnızca Apple platformları için lisanslı olduğu için kullanılmadı.
- İkonlar: Material Symbols Rounded, Apache License 2.0.
