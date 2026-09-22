# Menyiapkan mesin (Windows)

Panduan ini wajib diuji di laptop anggota lain. Panduan yang belum pernah diikuti orang kedua hampir selalu salah di suatu tempat.

Perkiraan waktu: 60–90 menit, sebagian besar menunggu unduhan.

## 1. Laragon: PHP, MySQL, Apache

Laragon 8.6.1 sudah terpasang di `D:\Laragon`. Yang dipakai proyek ini:

| Komponen | Versi | Path |
|---|---|---|
| PHP | **8.3.30** | `D:\Laragon\laragon\bin\php\php-8.3.30-Win32-vs16-x64\php.exe` |
| MySQL | 8.4.3 | `D:\Laragon\laragon\bin\mysql\mysql-8.4.3-winx64\bin\mysql.exe` |
| Apache | 2.4.66 | `D:\Laragon\laragon\bin\apache\httpd-2.4.66-260223-Win64-VS18` |
| Composer | 2.9.5 | `D:\Laragon\laragon\bin\composer\composer.phar` |

Kita memakai **PHP 8.3, bukan 8.5**, supaya sama dengan runner GitHub Actions. Beda versi PHP adalah sumber klasik bug "jalan di laptopku, gagal di CI".

Buka Laragon, klik kanan ikonnya, lalu **PHP → Version → php-8.3.30**.

Pastikan ekstensi `zip` aktif di `php.ini` milik PHP 8.3 (baris `extension=zip` tanpa titik koma). Verifikasi:

```bash
"/d/Laragon/laragon/bin/php/php-8.3.30-Win32-vs16-x64/php.exe" -m | grep -i zip
```

## 2. Database

```bash
MYSQL="/d/Laragon/laragon/bin/mysql/mysql-8.4.3-winx64/bin/mysql.exe"
"$MYSQL" -u root -e "CREATE DATABASE life_of_foods CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
"$MYSQL" -u root -e "CREATE DATABASE life_of_foods_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

Database kedua dipakai `php artisan test` supaya data pengembangan tidak ikut terhapus saat menjalankan uji.

## 3. Vhost `lof.test`

Repo sengaja tidak diletakkan di `D:\Laragon\laragon\www`, karena document root Laravel adalah `api/public`, bukan akar folder. Buat vhost manual di
`D:\Laragon\laragon\etc\apache2\sites-enabled\lof.test.conf`:

```apache
<VirtualHost *:80>
    DocumentRoot "D:/Proyek/life-of-foods/api/public"
    ServerName lof.test
    <Directory "D:/Proyek/life-of-foods/api/public">
        AllowOverride All
        Require all granted
    </Directory>
</VirtualHost>
```

Laragon menambahkan `lof.test` ke file hosts secara otomatis saat Apache di-restart. Uji dengan membuka `http://lof.test`.

## 4. JDK 21

Android Studio membawa JBR 25, tapi CI memakai **JDK 21**. Samakan supaya build tidak berbeda hasil.

Android Studio → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK → **Download JDK** → versi **21**, vendor Eclipse Temurin.

## 5. Android SDK

SDK ada di `C:\Users\<nama>\AppData\Local\Android\Sdk`, tapi baru berisi platform android-37 dan `cmdline-tools` masih kosong.

Android Studio → Settings → Languages & Frameworks → Android SDK:
- Tab **SDK Platforms**: centang **Android API 36**
- Tab **SDK Tools**: centang **Android SDK Command-line Tools (latest)** dan **Android SDK Build-Tools**

Platform API 27 tidak perlu diunduh. `minSdk 27` hanya menentukan versi Android terendah yang bisa memasang aplikasi, bukan SDK yang dipakai saat kompilasi.

## 6. Variabel lingkungan

```powershell
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx JAVA_HOME "C:\Users\$env:USERNAME\.jdks\temurin-21"
```

Sesuaikan path JDK dengan hasil unduhan di langkah 4. Tutup lalu buka lagi terminal supaya variabel terbaca.

## 7. GitHub CLI

Dibutuhkan skill `/qc` untuk push.

```powershell
winget install --id GitHub.cli
gh auth login --hostname github.com --git-protocol https --web
gh auth status
```

## 8. ngrok

ngrok sudah ikut Laragon di `D:\Laragon\laragon\bin\ngrok`. Daftar akun gratis, lalu:

```bash
ngrok config add-authtoken <token-dari-dashboard>
ngrok http 80 --host-header=lof.test
```

URL yang muncul dipakai sebagai `API_BASE_URL` di aplikasi Android. Tambahkan header `ngrok-skip-browser-warning: true` di OkHttp, kalau tidak ngrok akan menyisipkan halaman peringatan ke respons.

## 9. Verifikasi akhir

```bash
cd /d/Proyek/life-of-foods/api
php artisan migrate
php artisan test
```

Aplikasi Android: buka folder `android/` di Android Studio, tunggu Gradle sync, lalu **Run**. Kalau sync gagal dengan keluhan SDK, periksa `android/local.properties` memuat `sdk.dir` yang benar.
