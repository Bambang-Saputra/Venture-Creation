<?php

return [

    // Saat true, kode OTP ikut dikirim di respons POST /auth/otp/request dan
    // K03/M02 menampilkannya dengan spanduk "mode uji coba". Gateway WhatsApp
    // berbayar dan belum dipasang, jadi saat false permintaan OTP ditolak 503.
    'pilot_mode' => (bool) env('PILOT_MODE', false),

    // Rupiah penuh per pesanan. Nol selama pilot (orders.service_fee_rupiah).
    'service_fee' => (int) env('SERVICE_FEE', 0),

    'pesanan' => [
        'maks_qty_per_item' => 5,
        'maks_item' => 10,
        'maks_pesanan_aktif' => 3,
    ],

    'otp' => [
        'panjang' => 6,
        'berlaku_detik' => 300,
        'jeda_kirim_ulang_detik' => 60,
        'maks_percobaan' => 5,
    ],

];
