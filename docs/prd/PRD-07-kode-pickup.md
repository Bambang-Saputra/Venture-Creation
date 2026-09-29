# PRD-07 · Kode pickup

| | |
|---|---|
| Fitur | F-07 · MUST |
| Tujuan | Satu hal saja yang perlu ditunjukkan pembeli di toko |
| Pengguna | Pembeli |
| Layar | K14 |
| API | Kontrak §6: `GET /orders/{id}`, `POST /orders/{id}/cancel` |
| Status | API selesai (`PesananTest`, `TutupPesananLewatTest`) · Android: belum |

## Cerita pengguna

- Sebagai pembeli, saya ingin satu kode besar yang tinggal ditunjukkan ke kasir, supaya pengambilan cepat walau toko sedang ramai.
- Sebagai pembeli yang berhalangan datang, saya ingin membatalkan pesanan, supaya makanannya bisa dibeli orang lain.

## Alur

1. Setelah `POST /orders` → **K14** menampilkan kode pickup, nama dan alamat toko, jam ambil, item, total, dan cara bayar.
2. Selama K14 terbuka, `GET /orders/{id}` tiap 10 detik. Begitu statusnya `completed`, tampilan berubah menjadi "Selesai, selamat menikmati".
3. "Buka di Google Maps" → intent `geo:`.
4. "Batalkan pesanan" → konfirmasi → `POST /orders/{id}/cancel`.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Kode terdiri dari 6 karakter huruf dan angka, tanpa 0, O, 1, I, dan L, supaya kasir tidak salah baca | Ada di kode · `LayananPesanan::HURUF_KODE` |
| 2 | Kode dan detail pesanan hanya terlihat oleh pemesannya; pesanan orang lain dijawab 404 | Diuji · `daftar_dan_detail_hanya_milik_sendiri` |
| 3 | Kode hanya dikirim selama masih bisa ditukar | Ada di kode |
| 4 | Batal mengembalikan stok dan mencabut kode; batal ditolak kalau statusnya bukan `pending_pickup` | Diuji · `batal_mengembalikan_stok_dan_mencabut_kode` |
| 5 | Pesanan yang tidak diambil sampai 30 menit setelah jam ambil otomatis menjadi `no_show` | Diuji · `pesanan_tak_diambil_jadi_no_show_setelah_toleransi` |
| 6 | Setelah kasir menukar kode, K14 berubah menjadi selesai paling lambat 10 detik kemudian | Belum |
| 7 | Kode yang masih aktif tetap tampil tanpa internet, karena disimpan di perangkat. Sinyal di booth BIFEST tidak bisa diandalkan, sedangkan pembeli tetap harus bisa menunjukkan kodenya | Belum |
| 8 | Kode tampil besar dengan jarak antarhuruf yang lega | Belum |

## Kosong dan galat

- Pesanan sudah batal atau `no_show`: K14 menampilkan statusnya dengan jelas, tanpa kode.
- Gagal memuat saat offline: tampilkan kode yang tersimpan beserta keterangan "terakhir diperbarui jam …".

## Di luar lingkup

- QR code berisi kode, untuk M18 Pindai QR (COULD).
- Mengalihkan pesanan ke orang lain.
