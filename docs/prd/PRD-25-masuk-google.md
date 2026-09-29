# PRD-25 · Masuk dengan akun Google

| | |
|---|---|
| Fitur | F-25 · SHOULD |
| Tujuan | Masuk dengan satu ketukan, tanpa menunggu kode |
| Pengguna | Pembeli, dan mitra yang emailnya sudah terdaftar |
| Layar | Tombol "Lanjut dengan akun Google" di K02 dan M01 |
| API | Kontrak §3: `POST /auth/google` |
| Status | API selesai (`LoginGoogleTest`) · Android: tombol ada |
| Keputusan | ADR-0006 |

## Cerita pengguna

- Sebagai pembeli, saya ingin masuk dengan akun Google yang sudah ada di HP saya, supaya tidak perlu menunggu kode.

## Alur

1. **K02 / M01** → "Lanjut dengan akun Google" → Credential Manager → ID token dikirim ke `POST /auth/google`.
2. Pembeli baru → langsung ke K04 tanpa melewati K03. Pembeli lama → K07.
3. Mitra hanya bisa masuk kalau email Google-nya sama dengan email akun mitra yang sudah didaftarkan tim.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Tanda tangan palsu, `aud` salah, penerbit salah, token kedaluwarsa, dan email belum terverifikasi ditolak | Diuji · lima tes penolakan di `LoginGoogleTest` |
| 2 | Pembeli baru dibuatkan akun; masuk ulang dengan akun Google yang sama tidak membuat akun ganda | Diuji · `konsumen_baru_dibuatkan_akun`, `masuk_ulang_tidak_membuat_akun_ganda` |
| 3 | Login Google tidak pernah membuat akun mitra; email yang tidak dikenal ditolak 403 | Diuji · `mitra_dengan_email_tak_dikenal_ditolak` |
| 4 | Akun mitra terdaftar ditautkan saat masuk lewat Google pertama kali | Diuji · `mitra_terdaftar_ditautkan_saat_masuk_pertama` |
| 5 | Akun pembeli tidak bisa masuk sebagai mitra | Diuji · `akun_konsumen_tidak_bisa_masuk_sebagai_mitra` |
| 6 | Akun OTP lama tidak ditautkan otomatis lewat email, karena email di K04 tidak diverifikasi | Diuji · `email_akun_lama_tidak_ditautkan_otomatis` |

## Di luar lingkup

- Menautkan Google secara manual dari akun yang sedang masuk (`POST /me/google`, COULD).

## Catatan

Setiap anggota tim perlu mendaftarkan sidik jari SHA-1 debug-nya di Google Cloud Console. Tanpa itu Credential Manager menolak di perangkat orang tersebut. Hal ini akan terasa saat APK dari CI dipasang di HP anggota lain.
