# PRD-11 · Cocokkan kode pickup

| | |
|---|---|
| Fitur | F-11 · MUST |
| Tujuan | Menutup transaksi dengan satu tindakan |
| Pengguna | Kasir, pemilik toko |
| Layar | M12 (ketik kode), M18 (pindai QR, COULD) |
| API | Kontrak §10: `POST /pickup-codes/redeem` |
| Status | API selesai (`PesananMitraTest`) · Android: belum |

## Cerita pengguna

- Sebagai kasir, saya ingin mengetik kode dari HP pembeli dan langsung tahu apakah kodenya sah, supaya tidak ada orang yang mengambil tas milik orang lain.
- Sebagai pemilik, saya ingin setiap penukaran tercatat lengkap dengan nama kasirnya, supaya selisih di akhir hari bisa dilacak.

## Alur

1. **M12** → ketik 6 karakter (huruf kecil diterima) → `POST /pickup-codes/redeem`.
2. Berhasil → tampilkan isi pesanan, catatan, alergi, total, dan cara bayar → kasir menyerahkan pesanan dan menerima pembayaran.
3. Gagal → pesan yang menjelaskan alasannya, lalu input siap diketik ulang.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Kode sah menyelesaikan pesanan dalam satu transaksi: status `completed`, pembayaran `paid`, stok berpindah dari dipesan ke terjual, saldo berpindah dari menunggu ke tersedia, dan transaksi saldo tercatat | Diuji · `tukar_kode_menyelesaikan_pesanan_dan_memindahkan_stok_serta_saldo` |
| 2 | Kode yang sudah dipakai ditolak 409, dan pesannya menyebut jam pemakaian | Diuji · `kode_salah_batal_atau_lewat_ditolak` |
| 3 | Kode pesanan yang batal, atau yang ditukar lebih dari 30 menit setelah jam ambil, ditolak 409 | Diuji · `kode_salah_batal_atau_lewat_ditolak` |
| 4 | Kode milik toko lain tidak bisa ditukar; jawabannya 404 "kode tidak ada di toko ini" | Diuji · `kode_toko_lain_tidak_bisa_ditukar` |
| 5 | Setiap penukaran tercatat di `audit_logs` beserta identitas kasirnya | Ada di kode · `PesananMitraController` |
| 6 | Paling banyak 20 percobaan per menit | Ada di kode (throttle di rute) |
| 7 | Layar berhasil menampilkan alergi pembeli sekali lagi sebelum pesanan diserahkan | Belum |
| 8 | Pembeli yang masih membuka K14 melihat statusnya berubah menjadi selesai (PRD-07) | Belum |

## Kosong dan galat

- 404: "Kode tidak ditemukan di toko ini. Periksa lagi hurufnya." Jangan menyebut bahwa kode itu mungkin milik toko lain.
- 409 sudah dipakai: "Kode ini sudah ditukar jam 19.12." Tampilkan juga nama kasir yang menukarnya kalau tersedia.

## Di luar lingkup

- Pembayaran lewat aplikasi (ADR-0004).
- M18 memakai kamera (COULD). Endpoint-nya sama, dan QR cukup berisi kodenya.

## Catatan

Rencana awal meminta kode toko lain dijawab 403. Tim memilih 404 supaya keberadaan kode di toko lain tidak bocor. Keputusan ini lebih aman dan dipertahankan.
