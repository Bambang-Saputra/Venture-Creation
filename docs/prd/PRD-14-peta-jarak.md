# PRD-14 · Peta dan jarak tanpa API key

| | |
|---|---|
| Fitur | F-14 · SHOULD |
| Tujuan | Membantu pembeli memilih yang terdekat tanpa biaya Google Maps |
| Pengguna | Pembeli |
| Layar | K08 |
| API | Kontrak §4: `GET /listings?lat=&lng=&radius_km=` |
| Status | API selesai (`ListingTest`) · Android: belum |

## Cerita pengguna

- Sebagai pembeli, saya ingin melihat jualan yang bisa saya jangkau jalan kaki, supaya tidak memesan yang terlalu jauh.

## Alur

1. **K07** → "Peta" → **K08**.
2. Selama `maps_enabled` mati (bawaan): daftar urut jarak dengan pilihan radius 1, 3, 5, atau 10 km. Setiap baris punya tombol "Arahkan" yang membuka intent `geo:` dengan koordinat dan nama toko.
3. Saat `maps_enabled` menyala: peta Google dengan pin toko. Butuh API key yang dibatasi (ADR-0005).

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Urut dari yang terdekat dan dibatasi radius | Diuji · `urut_jarak_dan_radius` |
| 2 | Tanpa API key, K08 tetap berfungsi penuh sebagai daftar jarak | Belum |
| 3 | Intent `geo:` membuka aplikasi peta yang terpasang, dan membuka browser kalau tidak ada | Belum |
| 4 | Izin lokasi ditolak: pakai `area_label` dari profil, atau tampilkan daftar tanpa jarak | Belum |

## Di luar lingkup

- Rute dan perkiraan waktu tempuh.
- Peta di sisi mitra.

## Keputusan terbuka

API belum punya `GET /config`, padahal rencana awal meletakkan sakelar `maps_enabled` di sana. Pilih salah satu: tambahkan endpoint itu, atau tanam sakelarnya di `BuildConfig` Android.
