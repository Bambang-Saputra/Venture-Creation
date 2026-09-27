# ADR-0006 · Login Google sebagai jalur masuk kedua di samping OTP

**Status:** diusulkan · 27 September 2026

## Konteks
Figma K02 (Masuk) dan M01 (Masuk mitra) sama-sama punya tombol "Lanjut dengan akun Google" di bawah pembatas "atau", tetapi peta layar hanya memetakan kedua layar itu ke `POST /auth/otp/request`. Tim memutuskan tombol itu dibangun sebagai fitur, bukan sekadar tampilan.

Skema sekarang menjadikan `users.phone` wajib dan unik, karena satu-satunya jalur masuk adalah nomor HP + OTP. Akun Google tidak membawa nomor HP. K04 (Lengkapi profil) juga tidak meminta nomor HP.

## Keputusan
1. **Fitur F-25, prioritas SHOULD.** OTP tetap jalur kritis irisan vertikal minggu 5. Login Google tidak boleh menunda alur itu.
2. **Alur:** Android memakai Credential Manager (`GetGoogleIdOption`) dan mengirim ID token ke `POST /auth/google` dengan badan `{ "id_token": "...", "role": "consumer" | "partner" }`. Server memverifikasi tanda tangan token terhadap kunci publik Google (JWKS, di-cache), lalu memeriksa `aud` = `GOOGLE_CLIENT_ID`, `iss`, `exp`, dan `email_verified = true`. Respons sama dengan `POST /auth/otp/verify`: token Sanctum dan data user.
3. **Skema:** tambah `users.google_sub` (`varchar(255)`, nullable, unik) sebagai identitas Google. `sub` dipakai, bukan email, karena email akun Google bisa berubah. `users.phone` menjadi nullable; indeks unik tetap ada, dan MySQL membolehkan banyak `NULL` pada indeks unik.
4. **Konsumen:** `google_sub` yang belum dikenal membuat akun baru dengan `role = consumer`, nama dan email dari token, lalu diarahkan ke K04. Nomor HP opsional dan bisa ditambahkan belakangan lewat K19.
5. **Mitra:** login Google tidak pernah membuat akun mitra, karena M03 berstatus WON'T. Token hanya diterima kalau emailnya cocok dengan akun `partner` yang dibuat tim lewat `SeederPilot`. Pada login pertama `google_sub` ditautkan ke akun itu. Email tanpa akun mitra ditolak dengan `403` dan pesan "hubungi tim Life of Foods".
6. **Tidak ada penautan otomatis lewat email untuk akun konsumen OTP.** Email di K04 diisi bebas dan tidak diverifikasi. Menautkan akun Google ke akun lama berdasarkan email itu berarti siapa pun yang mengetik email orang lain di K04 bisa mengambil alih akunnya. Penautan manual dari akun yang sudah masuk (`POST /me/google`) dicatat sebagai COULD.

## Alasan
Satu ketukan tanpa menunggu WhatsApp memperpendek jalan masuk dan mengurangi ketergantungan pada gateway WhatsApp yang berbayar. OAuth client Google gratis dan tidak butuh akun penagihan, jadi keputusan ini tidak bertabrakan dengan ADR-0005. Verifikasi ID token di server menjaga aturan bahwa hanya server yang menerbitkan token Sanctum.

## Konsekuensi
- Migrasi baru: `phone` jadi nullable dan tambah `google_sub`, lengkap dengan `down()`. `down()` gagal kalau sudah ada user tanpa nomor HP; ini disengaja dan ditulis di komentar migrasi. `db/erd.md` dan `db/kamus-data.md` ikut diperbarui.
- Konfigurasi baru `GOOGLE_CLIENT_ID` (web client ID) di `api/.env.example`. Android memakai client ID yang sama sebagai `serverClientId`.
- Google Cloud Console butuh OAuth client tipe Android untuk nama paket beserta sidik jari SHA-1 debug dan rilis setiap anggota tim. Tanpa itu Credential Manager menolak di perangkat.
- Semua kode yang mengirim kabar lewat WhatsApp wajib menangani user tanpa `phone`.
- Uji: token palsu, `aud` salah, token kedaluwarsa, `email_verified = false`, mitra dengan email tak dikenal (`403`), dan login ulang dengan `sub` yang sama tidak membuat akun ganda.

## Alternatif yang ditolak
- **Tetap mewajibkan nomor HP setelah login Google** (Google, lalu OTP). Keunggulan login Google hilang karena user tetap menunggu kode WhatsApp.
- **Laravel Socialite dengan alur redirect web.** Alur itu dirancang untuk browser; aplikasi native cukup mengirim ID token.
- **Firebase Authentication.** Menambah dependensi dan konsol baru hanya untuk satu penyedia login.
- **Verifikasi token lewat endpoint `tokeninfo` Google.** Menambah satu panggilan jaringan di setiap login, dan Google sendiri tidak menyarankannya untuk produksi.
