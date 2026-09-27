# Life of Foods

Aplikasi penyalur makanan surplus dari bisnis F&B ke konsumen, sebelum berakhir di tempat sampah.
Proyek mata kuliah Venture Creation ENPR6312, BINUS.

Satu aplikasi Android dengan dua peran: **konsumen** membeli tas surplus atau menu satuan, **mitra** mencatat sisa harian lalu menjual yang tetap tersisa.

## Status

Uji coba. Akun mitra dibuat manual oleh tim lewat seeder, tanpa pendaftaran mandiri, tanpa verifikasi dokumen, dan tanpa pencairan dana sungguhan. Pembayaran dilakukan tunai atau QRIS milik mitra di tempat; aplikasi hanya mencatat.

## Tumpukan teknologi

| Bagian | Pilihan |
|---|---|
| Aplikasi | Android, **Java** + XML views, ViewBinding, Navigation Component, RecyclerView |
| Jaringan | Retrofit 2 + OkHttp + Gson |
| Gambar | Glide, dilayani dari `/storage` milik API |
| API | **Laravel 13**, PHP 8.3, Sanctum |
| Database | **MySQL 8.4**, InnoDB, utf8mb4 |
| SDK Android | `minSdk 27` (Android 8.1) · `compileSdk 36` · `targetSdk 36` |
| Peta | Mode gratis: jarak dihitung server + intent `geo:`; Google Maps di balik sakelar `maps_enabled` |
| Server uji | Laragon di laptop + ngrok, dengan cadangan hotspot LAN |
| CI | GitHub Actions: uji API, build APK debug, pemeriksaan judul PR |

## Struktur

```
api/       Laravel 13 — REST API /api
android/   aplikasi Android (Java)
db/        ERD, kamus data, dump demo
docs/      PRD, ADR, kontrak API, peta layar Figma, panduan tim, berkas kuliah
```

## Mulai dari mana

1. `docs/panduan/setup-windows.md` — menyiapkan PHP, MySQL, JDK, dan Android SDK
2. `docs/figma/peta-layar.md` — 44 layar Figma dipetakan ke fitur, endpoint, dan tabel
3. `docs/api/kontrak-api.md` — kontrak REST
4. `docs/prd/` — satu berkas per fitur
5. `docs/adr/` — keputusan teknis beserta alasannya

## Aturan kerja singkat

- Branch: `<tipe>/<lingkup>-<deskripsi>`, contoh `fitur/api-kode-pickup`. Hanya ada `main`.
- Commit: Conventional Commits berbahasa Indonesia, contoh `feat(api): tambah endpoint penukaran kode pickup`.
- **Push selalu lewat skill `/qc`**, yang membaca kode sebelum commit dan menolak atribusi AI.
- PR wajib: CI hijau, satu persetujuan, tangkapan layar untuk perubahan UI, squash merge.
- `.env` dan data mitra sungguhan tidak pernah masuk repo.

Selengkapnya di `docs/panduan/alur-kerja-git.md`.

## Desain

Prototipe Figma 44 layar: 23 layar konsumen (K01–K23) dan 21 layar mitra (M01–M21).
Perubahan warna, tipografi, jarak, dan komponen dilakukan di Figma lewat variables dan styles, bukan di layar satu per satu.
