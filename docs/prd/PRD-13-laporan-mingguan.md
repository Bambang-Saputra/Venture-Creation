# PRD-13 · Laporan mingguan mitra

| | |
|---|---|
| Fitur | F-13 · MUST |
| Tujuan | Memberi mitra alasan untuk kembali setiap minggu |
| Pengguna | Pemilik toko |
| Layar | M07 |
| API | Kontrak §11: `GET /partner/stores/{store}/reports/weekly?week_start=` |
| Status | API selesai (`CatatSisaTest`) · Android: belum |

## Cerita pengguna

- Sebagai pemilik, saya ingin melihat dalam satu layar berapa yang terbuang dan berapa yang terselamatkan minggu ini, supaya tahu apakah usaha saya mengurangi sisa berhasil.
- Sebagai pemilik, saya ingin tahu produk mana yang paling sering tersisa, supaya tahu mana yang perlu dikurangi dulu.

## Alur

1. **M07** → minggu ini (Senin sampai Minggu). Geser untuk melihat minggu sebelumnya.
2. Angka utama: nilai terbuang, nilai terselamatkan, jumlah pesanan, item terjual, dan perubahan dibanding minggu lalu.
3. Grafik harian. Hari yang tidak dicatat tampil sebagai celah, bukan nol.
4. Lima produk dengan sisa terbesar, masing-masing dengan tautan ke saran produksi (PRD-15).

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Laporan memuat nilai terbuang, nilai terselamatkan, dan lima produk dengan sisa terbesar | Diuji · `laporan_mingguan_sisa_terselamatkan_dan_produk_teratas` |
| 2 | Hanya pemilik yang bisa membuka laporan | Diuji · `laporan_hanya_untuk_pemilik` |
| 3 | Angka laporan sama persis dengan jumlah catatan harian minggu itu | Diuji sebagian lewat kriteria 1; perlu uji pembanding langsung |
| 4 | Hari tanpa catatan bernilai `null`, bukan 0, supaya tidak terbaca sebagai "tidak ada sisa" | Ada di kode |
| 5 | Grafik menampilkan celah untuk hari tanpa catatan | Belum |
| 6 | Kalau catatan minggu itu kurang dari 3 hari, tampilkan ajakan mencatat, bukan grafik yang hampir kosong | Belum |

## Kosong dan galat

- Minggu tanpa catatan sama sekali: "Belum ada catatan minggu ini. Catat sisa hari ini untuk memulai", beserta tombol ke M06.

## Di luar lingkup

- Ekspor PDF dan laporan bulanan. Ekspor CSV untuk hitungan COGS dan BEP dijadwalkan terpisah di minggu 10–11.

## Catatan

"Nilai terselamatkan" adalah uang yang kembali ke mitra dari pesanan yang sudah diambil. Nilai ini berbeda dari nilai asli makanannya. Sebut dengan jelas di layar, supaya angka ini tidak dikutip keliru di Business Report.
