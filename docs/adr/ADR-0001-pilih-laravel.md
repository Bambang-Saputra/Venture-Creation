# ADR-0001 · Memakai Laravel 13 untuk API

**Status:** diterima · 22 September 2026

## Konteks
Tenggat mati adalah BINUS Festival di minggu 9, sekitar lima minggu dari sekarang, dengan dua orang yang benar-benar menulis kode. API harus menyediakan autentikasi token, validasi masukan, migrasi skema yang bisa diulang di beberapa laptop, unggah gambar, dan penjadwalan agregasi mingguan.

## Keputusan
Memakai Laravel 13 di atas PHP 8.3 dengan Sanctum untuk token.

## Alasan
Tiga hal yang paling memakan jam di API — autentikasi token, validasi, dan migrasi — sudah tersedia di Laravel dan harus ditulis tangan di PHP native maupun Slim. Perkiraan penghematan sekitar 20 jam kerja, dan yang lebih penting, menghilangkan seluruh kelas bug autentikasi buatan sendiri.

CodeIgniter 4 adalah pilihan kedua yang sah, tapi Shield untuk token API menghabiskan keunggulan kesederhanaannya.

## Konsekuensi
- Tim perlu memahami struktur Laravel: FormRequest, Resource, Service, Policy.
- Hosting harus mendukung PHP 8.3 dan mengizinkan document root menunjuk ke `api/public`.
- Kita memakai PHP 8.3, bukan 8.5 yang aktif di Laragon, supaya sama dengan runner CI.

## Alternatif yang ditolak
PHP native dengan router sendiri, Slim 4, CodeIgniter 4.
