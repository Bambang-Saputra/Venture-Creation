# data-pilot

Berkas di folder ini memuat data mitra sungguhan: nama usaha, alamat, dan
nomor HP orang. Karena itu seluruh isi folder ini di-gitignore, kecuali
berkas contoh dan catatan ini.

## Cara pakai

1. Salin `contoh-mitra.csv.txt` menjadi `mitra.csv` di folder yang sama.
2. Isi satu baris per mitra. Kolom `tanggal_persetujuan` diisi tanggal mitra
   menandatangani surat persetujuan uji coba, format `YYYY-MM-DD`.
   Baris tanpa tanggal itu akan dilewati, bukan karena rewel, tapi karena
   tanggal itu satu-satunya bukti mitra memang setuju.
   Kolom `email_pemilik` opsional: isi dengan email akun Google pemilik
   kalau mitra ingin masuk lewat tombol Google di M01 (ADR-0006). Tanpa
   email itu, mitra hanya bisa masuk lewat OTP.
3. Jalankan dari folder `api/`:

   php artisan db:seed --class=SeederPilot

Menjalankannya dua kali tidak membuat toko kembar. Kuncinya nomor WhatsApp
pemilik, jadi perbaiki baris yang salah lalu jalankan ulang.

## Yang tidak boleh terjadi

- `mitra.csv` ikut ter-commit. Sudah dijaga `.gitignore`, dan dijaga lagi
  oleh gitleaks di CI, tapi tetap periksa `git status` sebelum push.
- Foto mitra diambil dari internet. Foto hanya milik mitra atau diambil tim.
- Nomor HP mitra muncul di log, screenshot, atau slide presentasi.
