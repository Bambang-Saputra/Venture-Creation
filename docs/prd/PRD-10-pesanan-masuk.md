# PRD-10 · Pesanan masuk mitra

| | |
|---|---|
| Fitur | F-10 · MUST |
| Tujuan | Memberi tahu mitra siapa yang akan datang, apa yang diambil, dan alergi apa yang dimiliki pembelinya |
| Pengguna | Pemilik toko, kasir |
| Layar | M11 (hari ini), M21 (riwayat, SHOULD) |
| API | Kontrak §10: `GET /partner/stores/{store}/orders` dengan `status=pending` atau `status=history` |
| Status | API selesai (`PesananMitraTest`) · Android: belum |

## Cerita pengguna

- Sebagai kasir menjelang jam ambil, saya ingin melihat semua pesanan beserta catatan alerginya di satu layar, supaya tas yang tepat sudah siap sebelum pembeli datang.
- Sebagai pemilik, saya ingin tahu pesanan mana yang dibayar tunai dan mana yang lewat QRIS, supaya rekonsiliasi di akhir hari cepat.

## Alur

1. **M11** → pesanan yang menunggu diambil hari ini, diurutkan menurut jam ambil.
2. Setiap kartu memuat nama depan pembeli, item, total, cara bayar, catatan, alergi, dan jam ambil.
3. "Cocokkan kode" → **M12** (PRD-11).
4. Selama layar terbuka, daftar diperbarui setiap 30 detik. Pesanan baru juga memicu notifikasi `pesanan_baru` (PRD-18).

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Pemilik dan kasir sama-sama bisa melihat pesanan masuk | Diuji · `pesanan_masuk_terlihat_kasir_dengan_catatan_dan_nama_depan` |
| 2 | Hanya nama depan pembeli yang tampil; nomor HP tidak | Diuji · `pesanan_masuk_terlihat_kasir_dengan_catatan_dan_nama_depan` |
| 3 | **Catatan dan alergi tampil langsung di kartu**, bukan disembunyikan di detail. Alergi `severe` ditandai paling mencolok. Menyimpan alergi tanpa menampilkannya sama saja dengan tidak punya fitur alergi | Data ada di API (`allergen_snapshot`) · tampilan Android belum |
| 4 | Kode pickup tidak ikut dikirim ke mitra; kasir harus mengetik kode dari HP pembeli | Ada di kode |
| 5 | Pesanan baru muncul di M11 paling lambat 30 detik setelah dibuat | Belum |

## Kosong dan galat

- Kosong: "Belum ada pesanan hari ini. Jualan yang aktif: {jumlah}", beserta tombol ke M10.
- Galat jaringan: pertahankan daftar terakhir dengan keterangan jam pembaruannya. Kasir tetap bisa bekerja dari daftar itu.

## Di luar lingkup

- Pembeli dan mitra saling berkirim pesan di dalam aplikasi.

## Keputusan terbuka

**Mitra belum bisa membatalkan pesanan.** Kalau stok fisik ternyata habis, misalnya terjual langsung di kasir, pesanan di aplikasi tetap menunggu sampai otomatis menjadi `no_show`, dan pembeli datang dengan sia-sia. Pilihan: endpoint batal untuk mitra dengan alasan wajib dan notifikasi ke pembeli, atau aturan tertulis bahwa stok di aplikasi dipisahkan secara fisik sejak jualan diterbitkan.
