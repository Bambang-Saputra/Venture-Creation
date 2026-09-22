# ADR-0004 · Menunda payment gateway sampai setelah BIFEST

**Status:** diterima · 22 September 2026

## Konteks
Rencana awal memuat Midtrans. Integrasi pembayaran menambah pendaftaran merchant, penanganan callback, rekonsiliasi, dan penanganan kegagalan pembayaran.

## Keputusan
Selama uji coba dan BIFEST, pembayaran dilakukan langsung dari konsumen ke mitra secara tunai atau QRIS statis milik mitra. Aplikasi hanya mencatat status pembayaran saat kode pickup ditukar. Midtrans Snap sandbox baru dikerjakan di minggu 12, dan hanya bila seluruh pekerjaan lain selesai.

## Alasan
Midtrans tidak mengubah satu pun komponen nilai mata kuliah, sementara pembayaran tunai di booth justru menghasilkan percakapan pelanggan yang lebih kaya. Tim juga tidak boleh memegang uang mitra selama pilot.

## Konsekuensi
- Kolom `service_fee` dan tabel `balance_transactions` tetap ditulis sekarang dengan nilai nol, supaya skema pendapatan bisa dihitung di BEP sebagai proyeksi.
- Tombol pencairan tampil nonaktif dengan teks "tersedia setelah masa uji coba".
- Rekonsiliasi dilakukan manual di akhir jam booth, bersandar pada `audit_logs` dan `balance_transactions`.

## Alternatif yang ditolak
Integrasi Midtrans sebelum BIFEST.
