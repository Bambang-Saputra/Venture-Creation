# PRD-04 · Filter alergen

| | |
|---|---|
| Fitur | F-04 · MUST |
| Tujuan | Membuat janji keamanan alergi bisa ditindaklanjuti, bukan sekadar label |
| Pengguna | Pembeli dengan alergi |
| Layar | K09, dan filter bawaan di K07 |
| API | Kontrak §4: `GET /listings?exclude_allergens[]=` |
| Status | API selesai (`ListingTest`) · Android: K09 belum |

## Cerita pengguna

- Sebagai pembeli yang alergi kacang, saya ingin jualan yang mengandung kacang tidak muncul sama sekali, supaya tidak perlu membaca kandungan satu per satu.
- Sebagai pembeli yang sedang memesankan untuk teman, saya ingin mematikan filter alergi saya untuk sementara, tanpa mengubah profil.

## Alur

1. **K07** → ikon filter → **K09**.
2. Alergi dari profil sudah tercentang. Pembeli bisa menambah atau melepas centang untuk pencarian ini saja.
3. "Terapkan" → kembali ke K07 dengan `exclude_allergens[]`.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Jualan yang ditandai "mengandung" **atau** "mungkin mengandung" alergen yang dihindari tidak tampil | Diuji · `alergen_yang_dihindari_disembunyikan_termasuk_mungkin_mengandung` |
| 2 | Alergi profil sudah tercentang saat K09 dibuka | Belum |
| 3 | Mengubah centang di K09 tidak mengubah profil alergi | Belum |
| 4 | Chip K09 dibangun dari `GET /allergens` bertipe `allergen` | Belum |
| 5 | Selama filter alergi aktif, K07 menampilkan penanda "Filter alergi aktif", supaya pembeli tahu ada jualan yang disembunyikan | Belum |

## Kosong dan galat

- Kosong karena filter: "Tidak ada jualan tanpa kacang tanah di sekitar Anda saat ini", beserta tombol perluas radius.

## Di luar lingkup

- Jaminan bebas kontaminasi silang. Aplikasi meneruskan pernyataan mitra, dan syarat penggunaan menyebut tim sebagai perantara.

## Keputusan terbuka

1. **Jualan tanpa data alergen lolos filter.** Filter hanya menyembunyikan jualan yang ditandai. Kalau mitra lupa mencentang, jualannya tampil sebagai aman bagi pembeli alergi, padahal statusnya "tidak diketahui", bukan "tidak mengandung".
   Usulan: saat menerbitkan (PRD-09), mitra wajib memilih secara sadar, yaitu mencentang alergen atau memilih "Tidak mengandung alergen umum". Daftar kosong tidak boleh jadi nilai bawaan. K10 tetap menampilkan `ingredients_text` utuh supaya pembeli bisa memeriksa sendiri.
2. **Filter vegetarian dan diet lain belum bisa.** Pilihan diet tersimpan di profil, tetapi jualan belum punya penanda diet (kontrak §12). Sementara ini K09 hanya menampilkan alergen. Pilihan diet tetap sampai ke mitra lewat `allergen_snapshot` di pesanan.
