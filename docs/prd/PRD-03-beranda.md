# PRD-03 · Beranda katalog surplus

| | |
|---|---|
| Fitur | F-03 · MUST |
| Tujuan | Menunjukkan apa yang bisa diselamatkan sekarang, yang terdekat lebih dulu |
| Pengguna | Pembeli |
| Layar | K07 (versi peta di K08, PRD-14) |
| API | Kontrak §4: `GET /listings` |
| Status | API selesai (`ListingTest`) · Android: K07 ada |

## Cerita pengguna

- Sebagai pembeli yang pulang kerja, saya ingin melihat makanan yang masih bisa diambil malam ini di dekat saya, supaya bisa memutuskan dalam satu menit.
- Sebagai pembeli yang sedang buru-buru, saya ingin menyaring jualan yang jam ambilnya segera berakhir, supaya tidak memesan yang tidak sempat saya ambil.

## Alur

1. Buka **K07** → `GET /listings` dengan `lat`/`lng` kalau izin lokasi diberikan, dan `exclude_allergens[]` dari profil (PRD-04).
2. Gulir ke bawah → halaman berikutnya lewat `next_page_url`, 20 jualan per halaman.
3. Chip cepat: jenis, kategori, halal, dan "Tutup kurang dari satu jam" (`ends_within_minutes`).
4. Ketuk kartu → **K10** untuk tas kejutan, **K11** untuk menu satuan.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Hanya jualan yang bisa dibeli sekarang yang tampil: aktif, stok ada, jam ambil belum lewat, toko tidak tutup sementara | Diuji · `hanya_jualan_yang_bisa_dibeli_yang_tampil` |
| 2 | Dengan lokasi, urut dari yang terdekat dan menampilkan jarak; radius 0,1–50 km | Diuji · `urut_jarak_dan_radius` |
| 3 | Filter jenis, kategori, halal, kata kunci, dan jam ambil bekerja bersamaan | Diuji · `filter_jenis_kategori_halal_cari_dan_jam_ambil` |
| 4 | Jualan yang habis dipesan langsung hilang dari beranda | Diuji · `stok_habis_mengubah_status_dan_hilang_dari_beranda` |
| 5 | Parameter tidak sah ditolak 422 | Diuji · `parameter_tidak_valid_ditolak` |
| 6 | Kartu memuat nama toko, judul, jenis, harga dengan harga asli dicoret, sisa stok, jam ambil, jarak, label halal, dan ikon alergen | Perlu dicek |
| 7 | Tanpa izin lokasi, beranda tetap tampil, hanya tanpa jarak | Perlu dicek |
| 8 | Setiap permintaan mengirim header `ngrok-skip-browser-warning`, supaya ngrok tidak membalas halaman HTML | Perlu dicek |

## Kosong dan galat

- Kosong: "Belum ada makanan surplus di sekitar Anda" beserta tombol perluas radius atau hapus filter. Kosong karena filter harus dibedakan dari kosong karena memang tidak ada jualan.
- Galat jaringan: pesan singkat dan tombol coba lagi; kartu yang sudah tampil tidak dihapus.

## Di luar lingkup

- Rekomendasi personal dan promosi berbayar.
- Foto jualan dari kamera mitra (F-20, belum ada endpoint unggah).
