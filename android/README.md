# JARVIS Mobil (Android) — APK'yı bilgisayarsız derlemek

Bu klasör JARVIS'in native Android uygulamasıdır (Kotlin + Jetpack Compose).
APK'yı **senin bilgisayarında Android Studio kurmadan**, GitHub'ın sunucuları derler.

## Bir kerelik kurulum (10 dakika)

1. **GitHub hesabı aç** (ücretsiz): https://github.com → Sign up.
2. **Yeni bir depo (repository) oluştur.**
   - Sağ üstte **+** → **New repository**.
   - İsim: `jarvis` (ne istersen).
   - **Önemli:** **Private** (gizli) seç. Çünkü imza anahtarı (`app/jarvis.keystore`) depoda duruyor.
   - **Create repository.**
3. **Dosyaları yükle.**
   - Depoda **Add file → Upload files**.
   - Bilgisayarındaki `jarvis_update` klasörünün içindekileri (özellikle `android/` ve `.github/`) sürükle bırak.
   - Alta **Commit changes.**
4. **Derleme kendiliğinden başlar.** Üstteki **Actions** sekmesine gir.
   - "JARVIS Mobil APK" işini göreceksin; sarı nokta = derleniyor, yeşil tik = bitti (ilk sefer ~5-8 dk).
5. **APK'yı indir.**
   - Yeşil tikli işe tıkla → en altta **Artifacts → JARVIS-Mobil-APK**. İndir, içinden `app-release.apk` çıkar.

## Telefona kurmak

1. APK'yı telefona at (WhatsApp'tan kendine gönder, kabloyla kopyala, ya da linki telefonda aç).
2. Dosyaya dokun → "Bilinmeyen kaynaklara izin ver" çıkarsa aç (sadece bu sefer).
3. Kur → aç → **İzin Sihirbazı** seni tek tek karşılar. En az **Mikrofon**'u ver; gerisini istersen sonra.

> Her yeni sürümde Actions yeni APK üretir. Aynı anahtarla imzalandığı için
> eski sürümün **üstüne** kurulur, veri gitmez (önce kaldırmana gerek yok).

## Bilgisayardaki JARVIS'e bağlamak (senin telefonun)

- Bilgisayarda JARVIS açıkken telefon QR'ını okut (V13'teki gibi). Uygulama linkteki adresi ve anahtarı kendi alır.
- Sonra **Ayarlar → "Anahtarları bilgisayardan al"** de: Gemini/Groq/Claude/OpenAI anahtarların şifreli olarak telefona iner, böylece **bilgisayar kapalıyken de** konuşabilirim.

## Arkadaşına kurmak (misafir)

- Arkadaşın aynı APK'yı kurar. QR okuttuğunda ona **misafir** anahtarı gider; kendi telefonunda bağımsız asistanı olur, senin verilerine karışmaz.
- 14.4'te gelecek "JARVIS Bulut" ile arkadaşların **hiç anahtar girmeden** kullanabilecek (anahtarlar APK'ya asla gömülmez).

## Nerede ne var

| Dosya | İş |
|---|---|
| `app/src/main/java/.../MainActivity.kt` | Ekranlar + kilit ekranı üstünde açılma |
| `.../servis/JarvisServisi.kt` | Arka planda "Jarvis" bekleyen ön plan servisi |
| `.../servis/Uyandirici.kt` | Kilitliyken ekranı açma (protokol 1) |
| `.../nebula/` | Telefona özel canlı Nebula (ekran + duvar kâğıdı) |
| `.../beyin/Sarjor.kt` | Groq → Gemini → Claude → OpenAI sıralı beyin |
| `.../eylem/Eylemler.kt` | Ara / uygulama aç / Spotify / bildirim oku |
| `.../izin/Izinler.kt` | İzin sihirbazının adımları |
| `.github/workflows/android.yml` | APK'yı derleyen GitHub Actions |
