# PRD-02 · Lengkapi profil dan profil alergi

| | |
|---|---|
| Fitur | F-02 · MUST |
| Tujuan | Alergi diisi sekali, lalu terbawa ke setiap pesanan |
| Pengguna | Pembeli |
| Layar | K04 → K05 → K06 → K07 |
| API | Kontrak §3: `PATCH /me`, `GET /allergens`, `PUT /me/allergens` |
| Status | API selesai (`ProfilTest`, `AlergenTest`) · Android: K04, K05 ada |

## Cerita pengguna

- Sebagai pembeli yang alergi kacang, saya ingin mencatat alergi sekali saja, supaya tidak perlu menjelaskannya lagi di setiap pesanan.
- Sebagai pembeli tanpa pantangan, saya ingin bisa melewati langkah ini, supaya cepat sampai ke beranda.

## Alur

1. **K04** isi nama (wajib) dan area (opsional) → `PATCH /me`.
2. **K05** pilih chip alergi dan pantangan dari `GET /allergens`. Setiap alergi bisa ditandai "parah" → `PUT /me/allergens`. Tombol "Lewati" tidak memanggil API.
3. **K06** onboarding, hanya sekali → **K07** beranda.
4. Alergi bisa diubah kapan saja di K19 (PRD-19).

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Nama wajib 2–120 karakter; nama dan area tidak bisa dikosongkan lagi setelah diisi | Diuji · `k04_melengkapi_profil_konsumen`, `nama_dan_area_tidak_boleh_dikosongkan` |
| 2 | Chip K05 dibangun dari `GET /allergens`, bukan ditulis tetap di aplikasi | API diuji · `daftar_hanya_alergen_aktif_berurutan_tanpa_token` · Android perlu dicek |
| 3 | Menyimpan alergi mengganti seluruh pilihan lama; daftar kosong mengosongkan | Diuji · `pilihan_baru_mengganti_yang_lama_dan_bisa_dikosongkan` |
| 4 | Kode tidak dikenal, ganda, atau nonaktif ditolak | Diuji · `kode_tidak_dikenal_ganda_atau_nonaktif_ditolak` |
| 5 | Mitra tidak bisa mengisi profil dan alergi pembeli | Diuji · `mitra_tidak_bisa_mengisi_profil_konsumen`, `mitra_ditolak` |
| 6 | Peran akun tidak bisa diubah lewat badan request | Diuji · `peran_tidak_bisa_diubah_lewat_badan_request` |
| 7 | Setiap pesanan menyalin alergi pembeli ke `allergen_snapshot`, sehingga mengubah profil tidak mengubah pesanan lama | Ada di kode · `LayananPesanan` |
| 8 | Alergi yang tersimpan otomatis menjadi filter bawaan beranda (PRD-04) | Belum |

## Kosong dan galat

- Galat validasi tampil di bawah input yang bersangkutan, memakai `errors.<field>[0]`.
- Daftar alergen gagal dimuat: tampilkan tombol coba lagi, jangan tampilkan layar kosong yang bisa disangka "tidak ada alergen".

## Di luar lingkup

- Verifikasi medis. Aplikasi mencatat pernyataan pembeli, bukan diagnosis.
- Tingkat kepedasan (chip "Tidak pedas" di Figma belum ada di skema).

## Catatan

Pilihan diet (vegetarian, vegan, tanpa babi, tanpa alkohol) ikut tersimpan dan ikut disalin ke pesanan, sehingga mitra membacanya. Pilihan itu belum bisa menyaring jualan. Lihat PRD-04.
