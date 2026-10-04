# Rencana lanjutan

Daftar pekerjaan setelah semua layar MUST selesai (per 4 Oktober 2026, `main` di #26).
Centang `[x]` begitu PR-nya masuk `main`, dan tulis nomor PR di belakangnya.
Kode layar mengikuti `docs/figma/peta-layar.md`.

## 1. Sebelum uji coba dan wawancara

- [ ] Uji ujung ke ujung di HP sungguhan dengan API asli: pembeli memesan, kasir di HP lain mencocokkan kode di M12, status di HP pembeli jadi "Selesai" dalam 10 detik.
- [ ] Cek foto jualan muncul di Beranda dan K10 dari APK terbaru.
- [ ] Cek "Populer hari ini" muncul setelah ada pesanan hari itu.
- [ ] Ganti "kode 6 digit" di form wawancara jadi "kode 6 karakter".
- [ ] Sebelum tiap sesi: nyalakan MySQL, API, `schedule:work`, dan ngrok. Kalau beda hari, isi ulang database demo (`migrate:fresh --seed`, menghapus pesanan lama).

## 2. Fitur baru dari masukan

- [x] **Rating dan ulasan.** Pembeli memberi bintang setelah pesanan selesai. Rating toko tampil di kartu jualan dan ikut dipakai untuk mengurutkan "Populer hari ini". Butuh tabel baru, karena belum ada di skema. Backend PR #28; Android: lembar "Beri ulasan" di K15 dan chip rating di K10/K11.
- [ ] **Iklan berbayar per hari tayang.** Terpisah dari "Populer hari ini". Tampil di hasil pencarian dan K09 dengan label "Iklan". Mitra memilih jualan dan jumlah hari. Tarif dan cara bayar (potong saldo atau bayar langsung) belum diputuskan. Filter alergi tetap berlaku untuk jualan yang diiklankan.
- [x] **Upload foto oleh mitra** (endpoint F-20). Backend sudah ada: foto profil, toko, template tas, dan jualan. Layar Android untuk mengunggah belum.

## 3. Layar SHOULD

- [x] M14 Profil toko dan M15 Pengaturan toko. Tombol keluar dipindah ke sini dari M05.
- [x] M21 Riwayat pesanan mitra (pakai ulang M11)
- [x] M08 Saran produksi (endpoint sudah ada, khusus pemilik)
- [x] M13 Saldo
- [x] K17 Notifikasi
- [x] K18-K20 Profil, Edit profil, Pengaturan pembeli
- [x] K06 Onboarding (halaman terakhir meminta izin lokasi)
- [x] K08 Peta (osmdroid dan OpenStreetMap, ditambah tab Daftar dengan `geo:`, lihat ADR-0005)
- [ ] Login Google K02/M01 (ADR-0006, butuh setup Google Cloud Console)

## 4. Layar COULD

- [ ] K16 Favorit (API sudah ada)
- [ ] K23 Bantuan
- [ ] M18 Pindai QR

## 5. Persiapan pilot sungguhan

- [ ] Hosting API dan MySQL di server sendiri (VPS atau Railway), supaya laptop tidak harus nyala terus.
- [ ] OTP lewat SMS atau WhatsApp, lalu matikan `PILOT_MODE`.
- [x] Kode galat khusus dari server untuk akun nonaktif, peran salah, dan batas 3 pesanan.
- [x] M11 memuat lebih dari 30 pesanan (paginasi). Tab Riwayat (M21) punya "Muat lebih banyak"; tab Hari ini masih halaman pertama saja.
- [ ] APK rilis yang ditandatangani, lalu Play Store (internal testing).
- [ ] Pembayaran online (ditunda, lihat ADR-0004).
