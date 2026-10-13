# JARVIS Mobil - bilgisayarda APK derleyici (Windows 10/11)
# GitHub'a gerek yok. Ilk sefer ~1.5 GB indirir (15-20 dk), sonraki derlemeler 1-3 dk.
# Her calistiginda en guncel kodu GitHub'daki acik depodan (mustafaozbal2/jarvis-mobil) ceker.
# Sonuc: Masaustunde JARVIS_Mobil.apk

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Base   = Join-Path $PSScriptRoot 'android_araclar'
$Sdk    = Join-Path $Base 'sdk'
$Log    = Join-Path $Base 'son_derleme.log'
$Depo   = 'https://github.com/mustafaozbal2/jarvis-mobil/archive/refs/heads/main.zip'
$JdkUrl = 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'
$CmdUrl = 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip'
$GrdUrl = 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip'
New-Item -ItemType Directory -Force -Path $Base | Out-Null

function Baslik($t) { Write-Host ''; Write-Host "==> $t" -ForegroundColor Green }
function Hata($t) { Write-Host ''; Write-Host "HATA: $t" -ForegroundColor Red; Read-Host 'Kapatmak icin Enter'; exit 1 }

function Indir($url, $dosya) {
    if (Test-Path $dosya) { return }
    Write-Host "   indiriliyor: $([IO.Path]::GetFileName($dosya)) ..."
    & curl.exe -L --fail --retry 3 -o "$dosya.part" $url
    if ($LASTEXITCODE -ne 0) { Remove-Item "$dosya.part" -ErrorAction SilentlyContinue; Hata "Indirilemedi: $url (internet baglantisini kontrol et)" }
    Move-Item "$dosya.part" $dosya
}

# Komutu gecici .cmd dosyasina yazip calistirir (cmd /c tirnak tuzaklarina dusmemek icin)
function Komut($satir) {
    $k = Join-Path $Base '_komut.cmd'
    Set-Content -Path $k -Value "@echo off`r`n$satir`r`n" -Encoding Default
    & $k | Out-Host
    return $LASTEXITCODE
}

function Ac($zip, $hedef) {
    New-Item -ItemType Directory -Force -Path $hedef | Out-Null
    & tar.exe -xf $zip -C $hedef
    if ($LASTEXITCODE -ne 0) { Hata "Acilamadi: $zip" }
}

Write-Host '   ___  ___  ______   _____ _____'   -ForegroundColor Cyan
Write-Host '  |_  |/ _ \ | ___ \ |  _  /  ___|'  -ForegroundColor Cyan
Write-Host '    | / /_\ \| |_/ / | | | \ `--.'   -ForegroundColor Cyan
Write-Host '    | |  _  ||    /  | | | |`--. \'  -ForegroundColor Cyan
Write-Host '/\__/ / | | || |\ \  \ \_/ /\__/ /'  -ForegroundColor Cyan
Write-Host '\____/\_| |_/\_| \_|  \___/\____/  MOBIL - APK DERLEYICI' -ForegroundColor Cyan

# ---------- 1) Java 17 ----------
Baslik 'Java 17'
$jdk = Get-ChildItem $Base -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jdk) {
    Indir $JdkUrl (Join-Path $Base 'jdk17.zip')
    Ac (Join-Path $Base 'jdk17.zip') $Base
    $jdk = Get-ChildItem $Base -Directory -Filter 'jdk-17*' | Select-Object -First 1
    if (-not $jdk) { Hata 'Java klasoru bulunamadi' }
}
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$($jdk.FullName)\bin;$env:Path"
Write-Host "   tamam: $($jdk.Name)"

# ---------- 2) Android SDK ----------
Baslik 'Android SDK'
$sdkm = Join-Path $Sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
if (-not (Test-Path $sdkm)) {
    Indir $CmdUrl (Join-Path $Base 'cmdtools.zip')
    $gecici = Join-Path $Base 'cmd_gecici'
    Remove-Item $gecici -Recurse -Force -ErrorAction SilentlyContinue
    Ac (Join-Path $Base 'cmdtools.zip') $gecici
    New-Item -ItemType Directory -Force -Path (Join-Path $Sdk 'cmdline-tools') | Out-Null
    Move-Item (Join-Path $gecici 'cmdline-tools') (Join-Path $Sdk 'cmdline-tools\latest')
    Remove-Item $gecici -Recurse -Force
}
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk
if (-not (Test-Path (Join-Path $Sdk 'platforms\android-35')) -or -not (Test-Path (Join-Path $Sdk 'build-tools\35.0.0'))) {
    Write-Host '   lisanslar kabul ediliyor...'
    $evet = Join-Path $Base 'evet.txt'
    Set-Content -Path $evet -Value (("y`r`n") * 40) -NoNewline
    Komut "call `"$sdkm`" --sdk_root=`"$Sdk`" --licenses < `"$evet`" > nul 2>&1" | Out-Null
    Write-Host '   platform 35 + build-tools 35 kuruluyor (birkac dakika)...'
    Komut "call `"$sdkm`" --sdk_root=`"$Sdk`" `"platforms;android-35`" `"build-tools;35.0.0`" `"platform-tools`" < `"$evet`"" | Out-Null  # ilerleme yine ekranda (Out-Host)
    if (-not (Test-Path (Join-Path $Sdk 'platforms\android-35'))) { Hata 'Android SDK kurulamadi' }
}
Write-Host '   tamam'

# ---------- 3) Gradle ----------
Baslik 'Gradle'
$gradle = Join-Path $Base 'gradle-8.11.1\bin\gradle.bat'
if (-not (Test-Path $gradle)) {
    Indir $GrdUrl (Join-Path $Base 'gradle.zip')
    Ac (Join-Path $Base 'gradle.zip') $Base
}
Write-Host '   tamam'

# ---------- 4) En guncel kod ----------
Baslik 'En guncel JARVIS kodu (GitHub)'
$kz = Join-Path $Base 'kaynak.zip'
$kd = Join-Path $Base 'kaynak'
Remove-Item $kz -Force -ErrorAction SilentlyContinue
Remove-Item $kd -Recurse -Force -ErrorAction SilentlyContinue
Indir $Depo $kz
Ac $kz $kd
$proje = Join-Path $kd 'jarvis-mobil-main\android'
if (-not (Test-Path $proje)) { Hata 'Kodda android klasoru yok' }
Write-Host '   tamam'

# ---------- 5) Imza sifresi ----------
$sf = Join-Path $Base 'imza_sifresi.txt'
if (-not (Test-Path $sf)) {
    Baslik 'Imza sifresi (yalnizca ilk sefer)'
    Write-Host '   GitHub''daki JARVIS_KEYSTORE_SIFRE degerini yapistir (sohbette verilen 24 karakterli sifre):'
    $s = (Read-Host '   Sifre').Trim()
    if ($s.Length -lt 8) { Hata 'Sifre bos ya da kisa' }
    Set-Content -Path $sf -Value $s -NoNewline
}
$env:JARVIS_KEYSTORE_SIFRE = (Get-Content $sf -Raw).Trim()

# ---------- 6) Derle ----------
Baslik 'APK derleniyor (ilk sefer 5-10 dk, sonra 1-3 dk)'
Push-Location $proje
$kod = Komut "call `"$gradle`" assembleRelease --no-daemon --console=plain > `"$Log`" 2>&1"
Pop-Location
if ($kod -ne 0) {
    Get-Content $Log -Tail 40
    Hata "Derleme basarisiz. Gunluk: $Log  (bu dosyayi Claude'a gonder)"
}

$apk = Get-ChildItem (Join-Path $proje 'app\build\outputs\apk\release') -Filter '*.apk' | Select-Object -First 1
if (-not $apk) { Hata 'APK bulunamadi' }
$masa = [Environment]::GetFolderPath('Desktop')
$hedef = Join-Path $masa 'JARVIS_Mobil.apk'
Copy-Item $apk.FullName $hedef -Force

Baslik 'HAZIR!'
Write-Host "   $hedef" -ForegroundColor Yellow
Write-Host '   Telefona at (WhatsApp ile kendine, ya da USB kablo), dokun, kur.'
Start-Process explorer.exe "/select,`"$hedef`""
Read-Host 'Kapatmak icin Enter'
