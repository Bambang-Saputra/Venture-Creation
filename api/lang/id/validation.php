<?php

/*
 * Pesan validasi bahasa Indonesia (APP_LOCALE=id). Tanpa berkas ini Laravel memakai pesan
 * bawaan bahasa Inggris, misalnya "The email has already been taken.", yang langsung tampil
 * di aplikasi. Kalimatnya ditulis untuk pembeli dan mitra, bukan untuk developer.
 */

return [
    'accepted' => ':Attribute perlu disetujui dulu.',
    'after' => ':Attribute harus sesudah :date.',
    'after_or_equal' => ':Attribute tidak boleh sebelum :date.',
    'array' => ':Attribute tidak valid.',
    'before' => ':Attribute harus sebelum :date.',
    'before_or_equal' => ':Attribute tidak boleh sesudah :date.',
    'between' => [
        'array' => ':Attribute harus berisi :min sampai :max pilihan.',
        'file' => 'Ukuran :attribute harus :min sampai :max KB.',
        'numeric' => ':Attribute harus antara :min dan :max.',
        'string' => ':Attribute harus :min sampai :max karakter.',
    ],
    'boolean' => ':Attribute tidak valid.',
    'confirmed' => 'Konfirmasi :attribute tidak cocok.',
    'date' => ':Attribute bukan tanggal yang benar.',
    'date_format' => 'Format :attribute tidak sesuai.',
    'digits' => ':Attribute harus :digits angka.',
    'digits_between' => ':Attribute harus :min sampai :max angka.',
    'distinct' => ':Attribute tidak boleh dipilih dua kali.',
    'email' => 'Format email belum benar, contoh nama@gmail.com.',
    'exists' => ':Attribute yang dipilih sudah tidak tersedia.',
    'file' => ':Attribute harus berupa berkas.',
    'filled' => ':Attribute wajib diisi.',
    'gt' => [
        'numeric' => ':Attribute harus lebih dari :value.',
    ],
    'gte' => [
        'numeric' => ':Attribute tidak boleh kurang dari :value.',
    ],
    'image' => ':Attribute harus berupa foto.',
    'in' => 'Pilihan :attribute tidak tersedia.',
    'integer' => ':Attribute harus berupa angka bulat.',
    'lt' => [
        'numeric' => ':Attribute harus kurang dari :value.',
    ],
    'lte' => [
        'numeric' => ':Attribute tidak boleh lebih dari :value.',
    ],
    'max' => [
        'array' => ':Attribute paling banyak :max pilihan.',
        'file' => 'Ukuran :attribute paling besar :max KB.',
        'numeric' => ':Attribute paling besar :max.',
        'string' => ':Attribute paling panjang :max karakter.',
    ],
    'mimes' => ':Attribute harus berupa berkas :values.',
    'min' => [
        'array' => ':Attribute paling sedikit :min pilihan.',
        'file' => 'Ukuran :attribute paling kecil :min KB.',
        'numeric' => ':Attribute paling sedikit :min.',
        'string' => ':Attribute paling sedikit :min karakter.',
    ],
    'numeric' => ':Attribute harus berupa angka.',
    'present' => ':Attribute wajib dikirim.',
    'prohibited' => ':Attribute tidak boleh diisi.',
    'regex' => 'Format :attribute belum benar.',
    'required' => ':Attribute wajib diisi.',
    'required_if' => ':Attribute wajib diisi.',
    'required_with' => ':Attribute wajib diisi bersama :values.',
    'required_without' => ':Attribute wajib diisi kalau :values kosong.',
    'size' => [
        'numeric' => ':Attribute harus :size.',
        'string' => ':Attribute harus :size karakter.',
    ],
    'string' => ':Attribute harus berupa teks.',
    'unique' => ':Attribute ini sudah dipakai akun lain.',
    'url' => 'Format :attribute belum benar.',
    'uuid' => ':Attribute tidak valid.',

    'custom' => [
        'email' => [
            'unique' => 'Email ini sudah dipakai akun lain. Coba email lain, atau masuk dengan akun yang memakai email ini.',
        ],
        'phone' => [
            'unique' => 'Nomor HP ini sudah terdaftar. Silakan masuk dengan nomor ini.',
            'regex' => 'Nomor HP belum benar, contoh 081234567890.',
        ],
    ],

    'attributes' => [
        'address' => 'alamat',
        'allergens' => 'alergen',
        'category' => 'kategori',
        'close_time' => 'jam tutup',
        'code' => 'kode',
        'comment' => 'ulasan',
        'content_hint' => 'perkiraan isi',
        'description' => 'deskripsi',
        'email' => 'email',
        'halal_certificate_no' => 'nomor sertifikat halal',
        'headline' => 'judul promosi',
        'ingredients_text' => 'kandungan',
        'items' => 'isi pesanan',
        'items.*.qty' => 'jumlah',
        'name' => 'nama',
        'nib' => 'NIB',
        'note' => 'catatan',
        'open_time' => 'jam buka',
        'original_value_rupiah' => 'harga normal',
        'owner_name' => 'nama pemilik',
        'payment_method' => 'cara bayar',
        'phone' => 'nomor HP',
        'pickup_end' => 'jam ambil selesai',
        'pickup_start' => 'jam ambil mulai',
        'price_rupiah' => 'harga jual',
        'qty_total' => 'jumlah',
        'rating' => 'rating',
        'reason' => 'alasan',
        'store_name' => 'nama usaha',
        'template_id' => 'cetakan tas',
        'title' => 'nama jualan',
        'whatsapp' => 'nomor WhatsApp',
    ],
];
