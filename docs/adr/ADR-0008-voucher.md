# ADR-0008: Voucher ditanggung Life of Foods, bayar tetap di toko

Status: diterima
Tanggal: 27 Oktober 2026

## Konteks

Masukan uji coba: pembeli perlu alasan untuk langsung membeli begitu membuka aplikasi. Contohnya pop up diskon besar yang bisa diklaim, dan game kecil yang memberi kupon. Aplikasi belum punya voucher sama sekali. Kolom `orders.discount_rupiah` sudah ada sejak skema awal, tapi selalu 0.

Selama pilot, pembayaran tetap dilakukan di toko (ADR-0004). Tim tidak memegang uang pembeli maupun mitra.

## Keputusan

1. **Voucher dibuat sungguhan di server, bukan hanya tampilan.**
   - `vouchers` menyimpan jenis voucher dengan sumber `popup`, `game`, atau `admin`.
   - `user_vouchers` menyimpan voucher milik pembeli; satu baris dipakai sekali.
   - `game_plays` membatasi Gosok & Menang sekali per pembeli per hari.
2. **Potongan ditanggung Life of Foods, bukan mitra.**
   - Pembeli membayar `total_rupiah` (sudah dipotong) di kasir.
   - Saat kode pickup ditukar, potongannya ditambahkan ke saldo toko sebagai `balance_transactions` jenis `adjustment` ("Voucher pembeli ditanggung Life of Foods"). Pendapatan mitra tetap utuh.
   - M11 dan M12 menampilkan potongan itu supaya kasir tidak bingung saat menerima uang kurang.
3. **Pemakaian voucher.**
   - Voucher dipakai lewat `user_voucher_id` di `POST /orders/preview` dan `POST /orders`.
   - Server memeriksa kepemilikan, masa berlaku, belum terpakai, dan minimal belanja.
   - Potongan persen dibatasi `max_discount_rupiah`, dan total tidak pernah negatif.
   - Pesanan yang dibatalkan pembeli mengembalikan vouchernya. Pesanan yang tidak diambil tidak mengembalikan voucher.
4. **Hadiah Gosok & Menang diundi server** menurut `weight`, dengan peluang "belum beruntung". Aplikasi hanya menampilkan hasil di bawah lapisan gosok, jadi hasilnya tidak bisa diakali dari HP.

## Akibat

- Selama pilot, subsidi voucher menjadi utang Life of Foods ke mitra. Utang itu terlihat di saldo toko, dan pencairannya mengikuti rencana pembayaran online (ADR-0004).
- Jumlah dan nilai voucher perlu dijaga tim, misalnya kuota dan masa klaim voucher pop up, supaya subsidi tidak membengkak.
- Data demo (`DataDemo::voucher`) membuat voucher pop up `WARHEMAT` dan empat hadiah Gosok & Menang.
