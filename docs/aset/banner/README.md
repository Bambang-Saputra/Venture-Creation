# Banner promosi

`banner.html` adalah sumber dua banner di `docs/aset/`:

| Alamat | Hasil | Ukuran |
|---|---|---|
| `banner.html#readme` | `banner-readme.png` | 1280×640, dirender 2x |
| `banner.html#promosi` | `banner-promosi.png` | 1080×1350 |

Font (Gabarito, Figtree) dan foto diambil langsung dari `android/app/src/main/res/font` dan `api/database/seeders/foto-demo`, jadi banner selalu memakai aset yang sama dengan aplikasi. Warnanya disalin dari `colors.xml`. Kalau warna merek berubah, ubah juga blok `:root` di `banner.html`.

Isi layar ponsel adalah tiruan HTML dari K07 Beranda, K14 Kode pickup, dan M05 Dashboard mitra, bukan tangkapan layar. Kalau teks di aplikasi berubah, sesuaikan di sini juga. Foto yang dipakai hanya yang berlisensi CC BY atau Pexels License, bukan CC BY-SA.

## Membuat ulang PNG

Buka `banner.html` di Chrome untuk melihat hasilnya, lalu jalankan dari akar repo (Git Bash):

```bash
CHROME="/c/Program Files/Google/Chrome/Application/chrome.exe"
SUMBER="file:///$(pwd -W)/docs/aset/banner/banner.html"
"$CHROME" --headless=new --hide-scrollbars --allow-file-access-from-files \
  --force-device-scale-factor=2 --window-size=1280,640 \
  --screenshot="$(pwd -W)/docs/aset/banner-readme.png" "$SUMBER#readme"
"$CHROME" --headless=new --hide-scrollbars --allow-file-access-from-files \
  --window-size=1080,1350 \
  --screenshot="$(pwd -W)/docs/aset/banner-promosi.png" "$SUMBER#promosi"
```

`--allow-file-access-from-files` diperlukan supaya font dan foto lokal ikut termuat.

## Social preview GitHub

Unggah `banner-readme.png` di Settings repo, bagian **Social preview**, supaya banner muncul saat tautan repo dibagikan.
