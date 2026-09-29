# PRD-19 · Profil dan pengaturan pembeli

| | |
|---|---|
| Fitur | F-19 · SHOULD |
| Tujuan | Pembeli memegang kendali atas data dan preferensinya sendiri |
| Pengguna | Pembeli |
| Layar | K18 (profil), K19 (ubah profil), K20 (pengaturan) |
| API | Kontrak §3: `GET` dan `PATCH /me`, `PUT /me/allergens`, `POST /auth/logout`, `DELETE /me` |
| Status | API selesai (`ProfilTest`, `SesiTest`, `AkunKonsumenTest`) · Android: belum |

## Cerita pengguna

- Sebagai pembeli, saya ingin mengubah alergi saya kapan saja, supaya filter dan catatan untuk mitra tetap benar.
- Sebagai pembeli yang tidak lagi memakai aplikasi, saya ingin menghapus akun, supaya data saya tidak tersimpan tanpa alasan.

## Alur

1. **K18** → nama, nomor, dan ringkasan pesanan.
2. **K19** → ubah nama, email, area, dan alergi.
3. **K20** → sakelar notifikasi, keluar, dan hapus akun.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | K20 cukup mengirim satu sakelar yang berubah | Diuji · `k20_cukup_mengirim_satu_sakelar` |
| 2 | Email yang sudah dipakai akun lain ditolak; koordinat harus dikirim berpasangan | Diuji · `email_milik_akun_lain_ditolak`, `koordinat_harus_berpasangan` |
| 3 | Keluar hanya mencabut token di perangkat ini | Diuji · `keluar_hanya_mencabut_token_perangkat_ini` |
| 4 | Hapus akun butuh konfirmasi, dan ditolak kalau masih ada pesanan yang belum diambil | Ada di kode |
| 5 | Akun tanpa riwayat pesanan dihapus penuh | Diuji · `hapus_akun_tanpa_pesanan_menghapus_penuh` |
| 6 | Akun yang pernah memesan dianonimkan, supaya laporan mitra tetap utuh; nomor yang sama bisa mendaftar lagi | Diuji · `hapus_akun_dengan_riwayat_pesanan_dianonimkan` |
| 7 | Akun mitra tidak bisa dihapus lewat jalur ini | Diuji · `hapus_akun_mitra_ditolak` |

## Di luar lingkup

- Mengganti nomor HP, karena butuh OTP ke nomor yang baru.
- Mengunduh salinan data pribadi.
- Menautkan akun Google ke akun OTP yang sudah ada (COULD, lihat ADR-0006).
