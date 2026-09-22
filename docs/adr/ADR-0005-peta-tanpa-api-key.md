# ADR-0005 · Peta berjalan tanpa API key sampai penagihan siap

**Status:** diterima · 22 September 2026

## Konteks
Google Maps Platform memerlukan akun penagihan dengan kartu kredit, yang belum tentu dimiliki anggota tim. Harga di luar kuota gratis perlu dicek sendiri di halaman harga resmi.

## Keputusan
Fitur peta dibangun di balik sakelar konfigurasi `maps_enabled`. Saat mati, aplikasi menampilkan daftar berurut jarak, dengan jarak dihitung di server memakai rumus Haversine, dan tombol yang membuka aplikasi Google Maps lewat `Intent(ACTION_VIEW, Uri.parse("geo:..."))`.

## Alasan
Nol biaya, nol API key, nol risiko tagihan tak terduga, dan pengunjung booth tetap mendapat pengalaman peta yang sudah mereka kenal. Arsitekturnya tetap siap kalau penagihan akhirnya aktif.

## Konsekuensi
- Kolom `latitude` dan `longitude` tetap ada di tabel `stores` sejak awal.
- Kalau sakelar dinyalakan, kunci Maps wajib dibatasi ke nama paket, sidik jari SHA-1 debug dan rilis, serta kuota harian. Kunci di dalam APK bisa diekstrak siapa pun; pembatasan itulah pertahanannya, bukan kerahasiaannya.

## Alternatif yang ditolak
Mengaktifkan penagihan Google Maps sejak awal, dan memakai OpenStreetMap dengan osmdroid.
