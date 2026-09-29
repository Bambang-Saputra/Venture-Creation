# PRD-05 · Detail jualan: kandungan, alergen, halal

| | |
|---|---|
| Fitur | F-05 · MUST |
| Tujuan | Memberi cukup informasi untuk memutuskan dengan aman |
| Pengguna | Pembeli |
| Layar | K10 (tas kejutan), K11 (menu satuan) |
| API | Kontrak §4: `GET /listings/{id}`, dan `GET /listings?store_id=&type[]=menu_item` untuk K11 |
| Status | API selesai (`ListingTest`) · Android: K10, K11 ada |

## Cerita pengguna

- Sebagai pembeli, saya ingin tahu perkiraan isi tas, kandungan, alergen, dan status halalnya, supaya berani membeli sesuatu yang isinya kejutan.
- Sebagai pembeli yang pilih-pilih, saya ingin membeli menu satuan satu per satu, supaya tidak membayar makanan yang tidak saya suka.

## Alur

**K10, tas kejutan:** baca detail → "Pesan" → K12. Tombol "Lihat menu satuan" → K11.

**K11, menu satuan:** atur jumlah per item (paling banyak sisa stok, dan paling banyak 5) → "Lanjut" → K13.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Detail memuat perkiraan isi, kandungan, alergen, jam ambil, dan data toko termasuk jam buka hari ini | Diuji · `detail_memuat_isi_toko_dan_ketersediaan` |
| 2 | Jualan yang habis atau lewat jam tetap bisa dibuka dengan tombol pesan mati; draf dijawab 404 | Diuji · `detail_habis_tetap_bisa_dibuka_draft_tidak` |
| 3 | Kandungan (`ingredients_text`) tampil utuh, tidak dipotong | Perlu dicek |
| 4 | Alergen tampil dengan tingkatnya: "mengandung" atau "mungkin mengandung" | Perlu dicek |
| 5 | Label halal tampil persis: `certified` → "Bersertifikat halal · No. {nomor}", `self_claim` → "Klaim mitra, belum bersertifikat", `not_stated` → "Status halal tidak disebutkan". Kata "halal" tidak pernah tampil tanpa keterangan | Perlu dicek |
| 6 | Alergen yang ada di profil pembeli ditandai sebagai peringatan di K10 dan K11 | Belum |
| 7 | Harga asli dicoret, dan persentase hemat ditampilkan | Perlu dicek |
| 8 | Tombol "Buka di Google Maps" memakai intent `geo:` dengan koordinat toko, tanpa API key (ADR-0005) | Perlu dicek |

## Kosong dan galat

- Jualan tidak ditemukan (404): "Jualan ini sudah tidak tersedia", lalu kembali ke beranda.
- K11 tanpa menu satuan di toko itu: sembunyikan tombol "Lihat menu satuan" di K10, jangan buka layar kosong.

## Di luar lingkup

- Foto asli jualan hari itu. Endpoint unggah foto belum ada (kontrak §12).
- Rating dan ulasan. Fitur sosial dilewati selama pilot.
