# ADR-0007 · Mitra mendaftar sendiri dari aplikasi, disetujui tim

**Status:** diterima · 7 Oktober 2026 (rencana lanjutan #7)

## Konteks
Sejak awal M03 Daftar usaha dan M04 Verifikasi dan rekening berstatus WON'T. Akun mitra hanya dibuat tim lewat `SeederPilot`, dan nomor yang belum terdaftar ditolak di halaman masuk mitra. Selama uji coba ke pemilik usaha, cara ini ternyata menghambat: calon mitra yang tertarik tidak bisa langsung mencoba, harus menunggu tim memasukkan datanya.

Figma M04 meminta foto KTP pemilik dan rekening pencairan. Keduanya data sensitif, dan selama pilot tim tidak memegang uang mitra (ADR-0004), jadi rekening belum dipakai sama sekali.

## Keputusan
1. **Nomor baru boleh masuk di halaman mitra.** Verifikasi OTP dengan `role=partner` membuat akun mitra tanpa toko. Akun itu hanya bisa membuka `GET`/`POST /partner/application`; semua rute `/partner/stores/{store}` tetap tertutup karena belum ada keanggotaan toko.
2. **Formulir dua langkah.** M03 untuk data usaha (nama, jenis, alamat, jam buka). M04 untuk pemilik dan izin (nama pemilik, NIB, nomor sertifikat halal).
3. **KTP tidak diminta.** Rekening pencairan juga tidak.
4. **NIB opsional.** Banyak usaha kecil belum punya NIB, atau izinnya atas nama orang tua. Kalau diisi, harus 13 angka, dan pendaftaran itu diperiksa lebih dulu.
5. **Tim yang menyetujui**, lewat `php artisan mitra:pendaftaran` dan `mitra:setujui {id}` (atau `--tolak="alasan"`). Toko baru dibuat saat disetujui: jam buka tiap hari dari formulir, pemilik sebagai anggota `owner`, saldo kosong, WhatsApp toko memakai nomor pendaftar.
6. **Login Google tetap tidak membuat akun mitra.** Pendaftaran baru hanya lewat nomor HP yang sudah lolos OTP.

## Alasan
Calon mitra bisa mencoba saat itu juga, sementara tim tetap memegang kendali atas siapa yang tampil ke pembeli. Persetujuan lewat artisan cukup untuk jumlah mitra pilot dan tidak butuh panel admin. Tidak meminta KTP berarti tidak ada data identitas sensitif yang harus disimpan dan dijaga.

## Konsekuensi
- Siapa pun bisa membuat akun mitra kosong. Dampaknya kecil: tanpa toko, akun itu tidak bisa melihat atau mengubah apa pun. Permintaan OTP tetap dibatasi per nomor.
- Tim harus rutin memeriksa `mitra:pendaftaran`. Belum ada notifikasi ke tim saat ada pendaftaran baru.
- Label halal `certified` hanya diberikan kalau nomor sertifikat diisi. Tim wajib memeriksa nomor itu sebelum menyetujui, karena aplikasi tidak pernah menulis "halal" tanpa dasar.
- Migrasi baru `partner_applications` dan kolom `stores.nib`.

## Alternatif yang ditolak
- **Tetap lewat `SeederPilot` saja.** Membuat setiap calon mitra menunggu tim.
- **Toko langsung aktif tanpa persetujuan.** Jualan dari usaha yang belum dikenal bisa langsung tampil ke pembeli, termasuk klaim halal dan kandungan yang belum diperiksa.
- **KTP wajib, seperti Figma M04.** Menyimpan identitas sensitif tanpa kebutuhan nyata selama pilot.
