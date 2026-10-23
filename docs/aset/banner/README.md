# Banner promosi

`banner.html` adalah sumber dua banner di `docs/aset/`:

| Alamat | Hasil | Ukuran |
|---|---|---|
| `banner.html#readme` | `banner-readme.png` | 1280×640, dirender 2x |
| `banner.html#promosi` | `banner-promosi.png` | 1080×1350 |

Font (Gabarito, Figtree) dan foto diambil langsung dari `android/app/src/main/res/font` dan `api/database/seeders/foto-demo`, jadi banner selalu memakai aset yang sama dengan aplikasi. Warnanya disalin dari `colors.xml`. Kalau warna merek berubah, ubah juga blok `:root` di `banner.html`.

Banner tidak memakai tangkapan layar aplikasi, tapi menyusun ulang komponen prototipe dengan ukuran dan warna yang sama: peta dengan penanda harga (K08), kotak "Populer hari ini" dan kartu jualan (K07), notifikasi (K17), ubin kategori, dan logo dari `drawable/ic_logo.xml`. Ikonnya disalin dari `drawable/ic_*.xml`. Bahasanya sengaja umum, tanpa istilah aplikasi seperti "tas kejutan" atau "kode pickup", supaya orang yang belum kenal aplikasinya langsung paham. Nama toko mengikuti data demo (`SeederDemo`); harganya hanya contoh. Foto yang dipakai hanya yang berlisensi CC BY, Pexels License, atau domain publik, bukan CC BY-SA.

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

Unggah `banner-readme.png` di Settings repo, bagian **Social preview**, supaya banner muncul saat tautan repo dibagikan. GitHub membatasi ukurannya maksimal 1 MB; kalau setelah diubah PNG-nya lebih besar dari itu, render dengan `--force-device-scale-factor=1` untuk keperluan ini.
