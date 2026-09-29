# PRD-15 · Saran produksi berbasis aturan

| | |
|---|---|
| Fitur | F-15 · SHOULD |
| Tujuan | Mengubah catatan sisa menjadi keputusan produksi untuk besok |
| Pengguna | Pemilik toko |
| Layar | M08 |
| API | Kontrak §9: `PATCH .../products/{product}` (`daily_production_qty`) · §11: `GET .../suggestions`, `POST .../accept`, `POST .../dismiss` |
| Status | API selesai (`DasborMitraTest`) · Android: belum |

## Cerita pengguna

- Sebagai pemilik, saya ingin saran jumlah produksi per produk beserta alasannya, supaya bisa memutuskan tanpa menghitung sendiri.

## Alur

1. **M08** → saran untuk besok, per produk: produksi sekarang, jumlah yang disarankan, selisihnya, perkiraan penghematan per minggu, dan alasannya.
2. Produk yang belum punya jumlah produksi harian → minta diisi dulu (`PATCH`).
3. "Terima" atau "Abaikan" untuk setiap saran.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Saran berbasis aturan yang bisa dijelaskan: rata-rata sisa pada hari yang sama dari catatan-catatan terakhir, bukan model prediksi | Diuji · `saran_dari_empat_sabtu_terakhir` |
| 2 | Kurang dari 3 catatan berarti tidak ada saran; yang tampil ajakan mencatat | Diuji · `data_kurang_dari_tiga_hari_tidak_memberi_saran` |
| 3 | Jumlah produksi harian bisa diisi dan dikosongkan | Diuji · `ubah_produksi_harian` |
| 4 | Setiap saran punya alasan dalam kalimat yang bisa dibaca mitra | Ada di kode |
| 5 | Menerima saran tidak mengubah apa pun secara otomatis; mitra tetap yang memutuskan | Ada di kode |

## Di luar lingkup

- Model prediksi atau machine learning.
- Saran harga jual.

## Catatan

Saran tanpa alasan akan diabaikan, dan memang pantas diabaikan. Alasan yang menyebut angka dari catatan mitra sendiri ("rata-rata sisa 6 setiap Sabtu") jauh lebih meyakinkan daripada angka yang dihitung diam-diam.
