# ADR-0005 · Peta berjalan tanpa API key sampai penagihan siap

**Status:** diterima · 22 September 2026 · direvisi 4 Oktober 2026 (peta di dalam aplikasi memakai osmdroid)

## Konteks
Google Maps Platform memerlukan akun penagihan dengan kartu kredit, yang belum tentu dimiliki anggota tim. Harga di luar kuota gratis perlu dicek sendiri di halaman harga resmi.

Figma K08 menggambar peta dengan penanda harga di dalam aplikasi. Versi pertama yang hanya berupa daftar dan tombol `geo:` dinilai belum cukup mirip dengan desain.

## Keputusan
- Peta di dalam aplikasi (tab Peta di K08) memakai **osmdroid** dengan ubin OpenStreetMap standar. Tidak perlu API key atau akun penagihan.
- Tab Daftar dan tombol rute tetap ada. Keduanya membuka aplikasi peta HP lewat `Intent(ACTION_VIEW, Uri.parse("geo:..."))`.
- Jarak tetap dihitung di server dengan rumus Haversine.

## Alasan
Nol biaya dan nol API key, tapi desain Figma K08 (peta, penanda harga, kartu terpilih) tetap bisa diikuti. Pengunjung booth juga masih bisa membuka rute di Google Maps yang sudah mereka kenal.

## Konsekuensi
- Kolom `latitude` dan `longitude` tetap ada di tabel `stores`, dan dikirim di `store` pada `GET /listings`.
- Kebijakan pemakaian ubin OSM (operations.osmfoundation.org/policies/tiles) wajib dipatuhi:
  - User-Agent berisi nama paket aplikasi; diatur di `PetaFragment`.
  - Ubin di-cache di folder cache aplikasi.
  - Atribusi "© OpenStreetMap" selalu tampil di pojok peta.
  - Tidak ada unduhan massal atau mode offline.
- Server ubin OSM tidak menjamin layanan untuk aplikasi dengan lalu lintas besar. Kalau pengguna tumbuh setelah BIFEST, ganti sumber ubin ke penyedia komersial atau server sendiri. Di kode, yang perlu diganti cukup `setTileSource`.
- Kalau nanti penagihan Google aktif dan tim ingin pindah ke Google Maps SDK, kunci Maps wajib dibatasi ke nama paket, sidik jari SHA-1 debug dan rilis, serta kuota harian. Kunci di dalam APK bisa diekstrak siapa pun; pembatasan itulah pertahanannya, bukan kerahasiaannya.

## Alternatif yang ditolak
- **Mengaktifkan penagihan Google Maps sejak awal:** butuh kartu kredit dan ada risiko tagihan.
- **Hanya daftar dan `geo:`:** ini versi pertama ADR ini. Ditinggalkan karena tidak sesuai dengan desain Figma K08.
