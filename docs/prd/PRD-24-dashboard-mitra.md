# PRD-24 · Dashboard mitra

| | |
|---|---|
| Fitur | F-24 · SHOULD |
| Tujuan | Satu layar yang menjawab "hari ini perlu mengerjakan apa" |
| Pengguna | Pemilik toko, kasir |
| Layar | M05 |
| API | Kontrak §11: `GET /partner/stores/{store}/summary` |
| Status | API selesai (`DasborMitraTest`) · Android: belum |

## Cerita pengguna

- Sebagai pemilik yang baru membuka aplikasi, saya ingin langsung tahu ada berapa pesanan menunggu dan apakah sisa hari ini sudah dicatat.

## Alur

1. Setelah M02 → **M05**: status buka atau tutup beserta menit sampai tutup, pesanan menunggu hari ini, dan pengingat catat sisa.
2. Angka minggu ini: nilai terselamatkan, item terjual, rata-rata sisa per hari, dan tas yang tidak terjual 7 hari terakhir.
3. Grafik harian dan hari tersibuk.
4. Pintasan ke M09 (pasang jualan), M11 (pesanan masuk), dan M06 (catat sisa).

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Ringkasan memuat status toko, angka minggu ini, grafik harian, dan hari tersibuk | Diuji · `ringkasan_dashboard` |
| 2 | Kalau sisa hari ini belum dicatat, dashboard menampilkan pengingat | Ada di kode (`is_today_logged`) |
| 3 | Angka minggu ini sama dengan yang tampil di laporan mingguan (PRD-13) | Belum ada uji pembanding |
| 4 | Pemilik dan kasir sama-sama bisa membuka dashboard | Ada di kode |

## Catatan

Rencana awal menaruh dashboard di COULD, sedangkan peta layar menaruhnya di SHOULD. PRD ini mengikuti peta layar, karena M05 adalah layar pertama setelah mitra masuk.
