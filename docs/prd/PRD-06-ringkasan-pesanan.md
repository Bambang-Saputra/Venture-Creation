# PRD-06 · Ringkasan pesanan dan catatan untuk mitra

| | |
|---|---|
| Fitur | F-06 · MUST |
| Tujuan | Meneruskan kebutuhan khusus pembeli ke mitra, lalu mengunci stok untuknya |
| Pengguna | Pembeli |
| Layar | K12 (tas kejutan), K13 (menu satuan) |
| API | Kontrak §6: `POST /orders/preview`, `POST /orders` |
| Status | API selesai (`PesananTest`) · Android: belum |

## Cerita pengguna

- Sebagai pembeli, saya ingin melihat total, jam ambil, dan peringatan alergi sebelum memesan, supaya tidak ada kejutan di toko.
- Sebagai pembeli yang alergi, saya ingin menulis catatan untuk mitra, supaya pesanan saya dipisahkan dari bahan yang saya hindari.

## Alur

1. Dari K10 atau K11 → `POST /orders/preview`. Tidak ada yang disimpan.
2. Tampilkan item, subtotal, biaya layanan (Rp0 selama pilot), total, jam ambil, alamat toko, dan peringatan alergi.
3. Isi catatan (opsional, paling banyak 300 karakter) dan pilih cara bayar di tempat: tunai atau QRIS toko.
4. "Buat pesanan" → `POST /orders` → K14.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Pratinjau menghitung total tanpa menyimpan apa pun dan mengembalikan `allergen_warnings` | Diuji · `pratinjau_menghitung_tanpa_menyimpan_dan_memberi_peringatan_alergi` |
| 2 | Peringatan alergi tampil sebelum tombol "Buat pesanan". Peringatan tidak memblokir, karena pembeli bisa saja memesankan untuk orang lain | Belum |
| 3 | Membuat pesanan memesan stok dan menerbitkan kode pickup dalam satu transaksi | Diuji · `buat_pesanan_memesan_stok_dan_menerbitkan_kode_pickup` |
| 4 | Satu pesanan hanya untuk satu toko, dan jam ambil semua item harus beririsan | Diuji · `satu_pesanan_satu_toko_dan_jam_ambil_harus_bertemu` |
| 5 | Paling banyak 3 pesanan yang belum diambil per pembeli | Diuji · `batas_pesanan_aktif` |
| 6 | Paling banyak 5 per item dan 10 item per pesanan | Ada di kode |
| 7 | Jualan yang sudah tidak tersedia ditolak | Diuji · `jualan_tidak_tersedia_ditolak` |
| 8 | Akun mitra tidak bisa memesan | Diuji · `mitra_tidak_bisa_memesan` |
| 9 | **Dua pembeli memesan unit terakhir bersamaan: tepat satu yang berhasil** | Ada di kode (`lockForUpdate` di `LayananPesanan`) · **belum ada uji** |
| 10 | Alergi pembeli disalin ke pesanan sebagai `allergen_snapshot` | Ada di kode |
| 11 | Tombol "Buat pesanan" nonaktif selama permintaan berjalan, supaya ketukan ganda tidak membuat dua pesanan | Belum |

## Kosong dan galat

- Stok habis saat menekan "Buat pesanan": tampilkan pesan dari `errors.items`, lalu kembali ke detail yang sudah diperbarui. Jangan hanya menampilkan Snackbar lalu diam.
- Batas 3 pesanan aktif: arahkan ke K15 supaya pembeli bisa mengambil atau membatalkan pesanan yang ada.

## Di luar lingkup

- Pembayaran di dalam aplikasi. Selama pilot pembeli membayar langsung ke mitra (ADR-0004).
- Voucher dan diskon (K22, WON'T).

## Catatan

- Kriteria 9 adalah yang oleh rencana awal disebut paling menentukan, karena kegagalannya berarti satu tas dijual ke dua orang di depan umum. Tulis ujinya dengan dua koneksi database terpisah, bukan dua panggilan berurutan.
- Rencana awal menjawab stok habis dengan `409 STOCK_UNAVAILABLE`. Implementasinya menjawab 422 dengan pesan di `errors.items`. Android mengikuti kontrak.
