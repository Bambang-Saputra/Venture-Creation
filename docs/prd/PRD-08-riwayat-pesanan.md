# PRD-08 · Riwayat pesanan pembeli

| | |
|---|---|
| Fitur | F-08 · MUST |
| Tujuan | Bukti pembelian, sekaligus jalan kembali ke kode yang masih aktif |
| Pengguna | Pembeli |
| Layar | K15 → K14 |
| API | Kontrak §6: `GET /orders?status=active` dan `?status=history`, `GET /orders/{id}` |
| Status | API selesai (`PesananTest`) · Android: belum |

## Cerita pengguna

- Sebagai pembeli yang menutup aplikasi setelah memesan, saya ingin menemukan kode saya lagi dalam dua ketukan.
- Sebagai pembeli, saya ingin melihat pesanan yang sudah lewat, supaya tahu berapa kali saya sudah menyelamatkan makanan.

## Alur

1. Tab bawah "Pesanan" → **K15** dengan dua tab: "Aktif" (`pending_pickup`) dan "Riwayat" (`completed`, `cancelled`, `no_show`).
2. Ketuk pesanan aktif → K14. Ketuk riwayat → detail tanpa kode.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Hanya pesanan milik sendiri yang tampil | Diuji · `daftar_dan_detail_hanya_milik_sendiri` |
| 2 | Status ditulis dengan bahasa sehari-hari: "Menunggu diambil", "Selesai", "Dibatalkan", "Tidak diambil" | Belum |
| 3 | Tab Aktif menampilkan jam ambil paling dekat di atas | Perlu dicek, urutan dari API belum dipastikan |
| 4 | 20 pesanan per halaman dengan gulir berkelanjutan | Ada di kode |
| 5 | Dari notifikasi pengingat ambil, ketukan langsung membuka K14 pesanan yang benar | Belum |

## Kosong dan galat

- Tab Aktif kosong: "Belum ada pesanan aktif" beserta tombol ke Beranda.
- Tab Riwayat kosong: "Pesanan yang sudah selesai akan muncul di sini."

## Di luar lingkup

- Ulasan setelah pesanan selesai. Fitur sosial dilewati selama pilot.
- Pesan ulang dengan satu ketukan.
