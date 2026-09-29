# PRD-01 · Masuk dengan nomor HP dan OTP

| | |
|---|---|
| Fitur | F-01 · MUST |
| Tujuan | Memberi identitas pembeli dan mitra tanpa memaksa kata sandi |
| Pengguna | Pembeli, pemilik toko, kasir |
| Layar | K01 → K02 → K03 (pembeli) · K01 → M01 → M02 (mitra) |
| API | Kontrak §3: `POST /auth/otp/request`, `POST /auth/otp/verify`, `POST /auth/logout`, `GET /me` |
| Status | API selesai (`OtpTest`, `SesiTest`) · Android: K01, K02, K03, M01, M02 ada |

## Cerita pengguna

- Sebagai pembeli, saya ingin masuk cukup dengan nomor HP, supaya tidak perlu membuat dan mengingat kata sandi baru.
- Sebagai pemilik toko, saya ingin masuk dengan nomor yang didaftarkan tim, supaya langsung sampai ke toko saya.
- Sebagai kasir, saya ingin masuk dengan nomor yang diundang pemilik, supaya tidak perlu memakai akun pemilik.

## Alur

1. **K01** pilih "Saya pembeli" atau "Saya mitra". Pilihan ini menentukan graf navigasi dan nilai `role`; tidak memanggil API.
2. **K02 / M01** isi nomor HP → `POST /auth/otp/request`.
3. **K03 / M02** isi kode 6 digit → `POST /auth/otp/verify` → token disimpan di SharedPreferences.
4. Pembeli dengan `is_new_user = true` → K04. Pembeli lama → K07. Mitra → `GET /partner/stores` → layar utama toko pertama.
5. Saat aplikasi dibuka lagi: `GET /me`. Jawaban 401 → hapus token, kembali ke K01.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | `08xx`, `+628xx`, `628xx`, dan `8xx` dinormalisasi menjadi `628…`; nomor yang bukan seluler Indonesia ditolak | Diuji · `nomor_dinormalisasi_dan_hanya_hash_yang_disimpan`, `nomor_bukan_seluler_indonesia_ditolak` |
| 2 | Database hanya menyimpan hash kode, tidak pernah kode mentah | Diuji · `nomor_dinormalisasi_dan_hanya_hash_yang_disimpan` |
| 3 | Kode berlaku 5 menit dan hanya bisa dipakai sekali | Diuji · `kode_kedaluwarsa_setelah_lima_menit`, `kode_hanya_bisa_dipakai_sekali` |
| 4 | Lima kali salah mengunci kode itu | Diuji · `lima_kode_salah_mengunci_kode_itu` |
| 5 | Kirim ulang harus menunggu jeda, dan kode lama langsung gugur | Diuji · `kirim_ulang_menunggu_jeda_dan_kode_lama_gugur` |
| 6 | Selama `PILOT_MODE=true`, kode tampil di K03 dengan spanduk "mode uji coba"; tanpa mode pilot, permintaan ditolak 503 | API diuji · `tanpa_mode_pilot_permintaan_ditolak_503` · spanduk Android perlu dicek |
| 7 | Nomor yang belum terdaftar sebagai mitra tidak dikirimi kode di halaman mitra | Diuji · `mitra_yang_belum_terdaftar_tidak_dikirimi_kode` |
| 8 | Nomor mitra yang masuk lewat halaman pembeli diarahkan ke halaman mitra | Diuji · `nomor_mitra_ditolak_di_halaman_konsumen` |
| 9 | Pembeli baru dibuatkan akun; mitra terdaftar masuk tanpa akun baru | Diuji · `konsumen_baru_dibuatkan_akun`, `mitra_terdaftar_masuk_tanpa_akun_baru` |
| 10 | Akun nonaktif ditolak, termasuk token yang sudah terbit | Diuji · `akun_nonaktif_ditolak`, `token_akun_yang_dinonaktifkan_ditolak` |
| 11 | Keluar hanya mencabut token di perangkat ini | Diuji · `keluar_hanya_mencabut_token_perangkat_ini` |
| 12 | Kode OTP tidak pernah tertulis di log | Belum ada uji |

## Kosong dan galat

- Kode salah: pesan menyebut sisa percobaan, input kode dikosongkan.
- 429 saat kirim ulang: tombol "Kirim ulang" menampilkan hitung mundur, bukan pesan galat.
- Tanpa internet: pesan singkat dan tombol coba lagi; nomor yang sudah diketik tidak hilang.

## Di luar lingkup

- Pengiriman OTP lewat WhatsApp. Gateway-nya berbayar, jadi selama pilot kode tampil di layar.
- Pendaftaran mitra mandiri (M03, M04). Akun mitra dibuat tim lewat `SeederPilot` atau diundang pemilik toko.
- Kata sandi dan lupa kata sandi.

## Catatan

Masuk dengan Google adalah jalur kedua di layar yang sama. Lihat PRD-25.
