# ADR-0002 · Aplikasi Android memakai Java dan XML views

**Status:** diterima · 22 September 2026

## Konteks
Tim sudah menguasai Java dari mata kuliah pemrograman, dan belum pernah memakai Kotlin. Jetpack Compose adalah plugin compiler Kotlin, sehingga tidak bisa dipakai dari Java. Lingkup MUST mencakup sekitar 20 layar yang harus jalan sebelum minggu 9.

## Keputusan
Java dengan XML layout, ViewBinding, Fragment, Navigation Component, dan RecyclerView. Jaringan memakai Retrofit dengan callback, penyimpanan lokal memakai SharedPreferences, gambar memakai Glide.

## Alasan
Menulis Compose sambil belajar Kotlin dan coroutines, sambil dikejar BIFEST, adalah risiko yang tidak sebanding. Familiar mengalahkan elegan saat tenggatnya mati. Tutorial Android berbahasa Indonesia juga paling banyak memakai Java dan XML, yang penting saat tim buntu di tengah malam.

## Konsekuensi
- Kode UI bertambah sekitar 30–40 persen dibanding Compose, hampir semuanya di adapter RecyclerView.
- Mitigasinya satu pola layar dan satu `BaseListAdapter` generik yang disalin ke semua layar, didokumentasikan di `docs/panduan/pola-layar-java.md`.
- Tanpa coroutines, pemanggilan API memakai `enqueue` dengan callback dan hasilnya dikirim lewat `LiveData`.
- Kalau nanti ada anggota yang menguasai Kotlin, migrasi bisa dilakukan per layar karena keduanya bisa hidup berdampingan. Itu bukan pekerjaan semester ini.

## Alternatif yang ditolak
Kotlin dengan Compose, Kotlin dengan XML views, dan Flutter.
