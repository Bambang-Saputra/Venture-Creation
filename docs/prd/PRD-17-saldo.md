# PRD-17 · Saldo, baca saja

| | |
|---|---|
| Fitur | F-17 · SHOULD |
| Tujuan | Mitra melihat hasil penjualannya, tanpa tim memegang uang mereka |
| Pengguna | Pemilik toko |
| Layar | M13 (M20 Konfirmasi pencairan: WON'T) |
| API | Kontrak §8: `GET .../balance`, `GET .../balance/transactions` |
| Status | API selesai (`TokoTest`) · Android: belum |

## Cerita pengguna

- Sebagai pemilik, saya ingin melihat berapa yang sudah terjual lewat aplikasi, supaya bisa mencocokkannya dengan uang di laci kasir.

## Alur

1. **M13** → saldo tersedia, saldo menunggu diambil, total sejak bergabung, dan item terjual minggu ini.
2. Di bawahnya, daftar transaksi: judul pesanan, kode pickup, jumlah, dan saldo setelahnya.
3. Tombol "Cairkan" tampil tapi nonaktif, dengan teks dari `withdrawal.reason`.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Saldo tersedia berasal dari pesanan yang kodenya sudah ditukar; saldo menunggu berasal dari pesanan yang belum diambil | Diuji · `saldo_dan_riwayat_transaksi` |
| 2 | Kasir tidak bisa melihat saldo; di detail toko nilainya `null` | Diuji · `kasir_bisa_melihat_toko_tanpa_saldo`, `kasir_ditolak_ubah_toko_saldo_dan_anggota` |
| 3 | Tombol "Cairkan" nonaktif dengan teks "Pencairan tersedia setelah masa uji coba" | API ada · Android belum |
| 4 | Riwayat transaksi hanya bertambah, tidak pernah diubah; koreksi ditulis sebagai baris baru | Ada di kode |

## Di luar lingkup

- Pencairan dana sungguhan (M20, WON'T).

## Catatan

Selama pilot pembeli membayar langsung ke mitra, tunai atau lewat QRIS milik mitra. Angka di M13 adalah catatan penjualan, bukan uang yang dipegang tim. Tulis ini di layar, supaya mitra tidak mengira ada dana yang tertahan.
