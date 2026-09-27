<?php

return [

    // Saat true, kode OTP ikut dikirim di respons POST /auth/otp/request dan
    // K03/M02 menampilkannya dengan spanduk "mode uji coba". Gateway WhatsApp
    // berbayar dan belum dipasang, jadi saat false permintaan OTP ditolak 503.
    'pilot_mode' => (bool) env('PILOT_MODE', false),

    'otp' => [
        'panjang' => 6,
        'berlaku_detik' => 300,
        'jeda_kirim_ulang_detik' => 60,
        'maks_percobaan' => 5,
    ],

];
