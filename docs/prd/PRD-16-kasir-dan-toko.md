# PRD-16 · Kasir dan pengaturan toko

| | |
|---|---|
| Fitur | F-16 · SHOULD |
| Tujuan | Pemilik bisa membagi tugas tanpa membagi akses ke uang |
| Pengguna | Pemilik toko, kasir |
| Layar | M14 (profil toko), M15 (pengaturan dan anggota) |
| API | Kontrak §8: `GET` dan `PUT /partner/stores/{store}`, `GET`, `POST`, dan `DELETE .../members` |
| Status | API selesai (`TokoTest`) · Android: belum |

## Cerita pengguna

- Sebagai pemilik, saya ingin mengundang kasir dengan nomor HP-nya, supaya kasir bisa mencocokkan kode tanpa melihat saldo.
- Sebagai pemilik, saya ingin menutup toko sementara, supaya jualan saya hilang dari aplikasi selama libur.

## Alur

1. **M15** → "Tambah kasir" → isi nomor dan nama → `POST .../members` → bagikan `invite_message` lewat intent WhatsApp.
2. Kasir masuk lewat M01 dan M02 dengan nomor itu.
3. "Cabut" → akses kasir ke toko ini hilang saat itu juga.
4. **M14** → ubah jam buka, alamat, label halal, kandungan bawaan, dan sakelar "Tutup sementara".

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Kasir boleh mencatat sisa, melihat pesanan masuk, mencocokkan kode, dan melihat dashboard | Diuji · `kasir_bisa_melihat_toko_tanpa_saldo` dan tes modul terkait |
| 2 | Kasir ditolak mengubah toko, melihat saldo, mengelola anggota, dan memasang jualan | Diuji · `kasir_ditolak_ubah_toko_saldo_dan_anggota`, `hanya_pemilik_yang_bisa_mengelola` |
| 3 | Penolakan untuk kasir tampil sebagai pesan sopan, bukan crash | Belum |
| 4 | Mengundang kasir membuat akun mitra untuk nomor itu | Diuji · `undang_kasir_membuat_akun_mitra` |
| 5 | Undangan ditolak untuk nomor akun pembeli, anggota yang sudah ada, dan pemilik | Diuji · `undang_ditolak_untuk_konsumen_anggota_dan_pemilik` |
| 6 | Kasir yang dicabut bisa diundang ulang; pemilik tidak bisa dicabut | Diuji · `cabut_kasir_lalu_undang_ulang`, `pemilik_tidak_bisa_dicabut` |
| 7 | Label `certified` wajib disertai nomor sertifikat | Diuji · `validasi_ubah_toko` |
| 8 | Tutup sementara menyembunyikan semua jualan toko dari pembeli | Diuji · `pemilik_ubah_jam_dan_tutup_sementara` |
| 9 | Perubahan toko dan keanggotaan tercatat di `audit_logs` | Ada di kode · `TokoController`, `AnggotaTokoController` |

## Di luar lingkup

- Rekening pencairan dan dokumen verifikasi (M04, WON'T).
- Rating toko dan lencana terverifikasi.
