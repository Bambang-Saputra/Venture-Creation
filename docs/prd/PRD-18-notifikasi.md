# PRD-18 · Notifikasi dan pengingat ambil

| | |
|---|---|
| Fitur | F-18 · SHOULD |
| Tujuan | Pembeli tidak lupa mengambil pesanan, dan mitra tidak melewatkan pesanan baru |
| Pengguna | Pembeli, pemilik toko, kasir |
| Layar | K17, dan lencana lonceng di beranda |
| API | Kontrak §7: `GET /notifications`, `POST /notifications/{id}/read`, `POST /notifications/read-all` |
| Status | API selesai (`AkunKonsumenTest`) · Android: belum |

## Cerita pengguna

- Sebagai pembeli, saya ingin diingatkan 30 menit sebelum jam ambil, supaya makanan yang sudah saya pesan tidak terbuang juga.
- Sebagai kasir, saya ingin tahu begitu ada pesanan baru, supaya bisa menyiapkannya.

## Alur

1. Server membuat notifikasi saat kejadian berikut: `pesanan_baru` (untuk mitra), `pengingat_ambil` (30 menit sebelum jam ambil), `porsi_terselamatkan` (kode ditukar), `mitra_favorit_memasang` (toko favorit menerbitkan jualan).
2. Aplikasi mengambil notifikasi saat layar dibuka atau saat aplikasi kembali ke depan. Lencana lonceng memakai `unread_count`.
3. Ketuk notifikasi → layar tujuan dari `data.screen`.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Pesanan baru memberi tahu pemilik dan kasir | Diuji · `pesanan_baru_memberi_tahu_pemilik_dan_kasir` |
| 2 | Menukar kode memberi tahu pembeli | Diuji · `tukar_kode_memberi_tahu_porsi_terselamatkan` |
| 3 | Pengingat ambil terkirim sekali per pesanan, dan hanya kalau sakelarnya menyala | Diuji · `pengingat_ambil_sekali_per_pesanan` |
| 4 | Kabar toko favorit paling banyak sekali per toko per hari, dan mengikuti sakelar | Diuji · `mitra_favorit_memasang_paling_banyak_sekali_sehari_dan_mengikuti_sakelar` |
| 5 | Tandai dibaca bekerja; notifikasi milik orang lain dijawab 404 | Diuji · `tandai_dibaca_dan_notifikasi_orang_lain` |
| 6 | Pengingat ambil juga dijadwalkan di perangkat lewat WorkManager, supaya tetap muncul walau aplikasi tertutup | Belum |

## Di luar lingkup

- Push notification lewat FCM (ditunda).
- Kabar lewat WhatsApp.

## Catatan

Tanpa FCM, notifikasi di server hanya terlihat saat aplikasi dibuka. Karena itu kriteria 6 penting: pengingat ambil adalah satu-satunya notifikasi yang harus sampai walau aplikasi sedang tertutup.
