# PRD-09 · Mitra memasang tas kejutan dan menu satuan

| | |
|---|---|
| Fitur | F-09 · MUST |
| Tujuan | Mengubah sisa hari ini menjadi jualan dalam waktu kurang dari dua menit |
| Pengguna | Pemilik toko |
| Layar | M09 (pasang tas), M10 (kelola jualan), M16 (pasang menu satuan), M17 (kelola menu satuan) |
| API | Kontrak §9: `GET .../templates`, `GET .../products`, `POST .../listings`, `POST .../publish`, `POST .../pause`, `GET .../listings` |
| Status | API selesai (`ListingMitraTest`) · Android: belum |

## Cerita pengguna

- Sebagai pemilik bakery jam 18.00, saya ingin memasang tas kejutan dari cetakan dengan satu ketukan, supaya tidak mengetik ulang setiap hari.
- Sebagai pemilik kafe, saya ingin menjual croissant sisa satu per satu dengan harganya masing-masing, supaya pembeli bisa memilih.

## Alur

**Tas kejutan (M09):** pilih cetakan → ubah jumlah dan jam ambil kalau perlu → "Terbitkan". Tanpa cetakan ("tas campur"): isi judul, harga, perkiraan isi, kandungan, alergen, dan label halal.

**Menu satuan (M16):** pilih produk → isi jumlah dan harga per produk → "Terbitkan". Satu produk menjadi satu jualan, supaya stoknya berkurang per produk.

**Kelola (M10, M17):** lihat sisa stok dan potensi pendapatan per jualan; jeda atau terbitkan lagi.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Tas dari cetakan terbit dalam satu ketukan | Diuji · `m09_terbitkan_tas_dari_template_dalam_satu_ketukan` |
| 2 | Menu satuan: satu produk menjadi satu jualan | Diuji · `m16_menu_satuan_satu_listing_per_item` |
| 3 | Jualan tanpa kandungan tidak bisa terbit, tapi bisa disimpan sebagai draf. Database ikut menolak lewat `chk_listings_kandungan` | Diuji · `tanpa_kandungan_tidak_bisa_terbit_tapi_bisa_disimpan_draf`, `SkemaTest` |
| 4 | Jam ambil yang tidak sah atau sudah lewat ditolak | Diuji · `jam_ambil_tidak_valid_ditolak` |
| 5 | Jeda hanya menghentikan pesanan baru; pesanan yang sudah masuk tetap berlaku | Diuji · `jeda_lalu_terbitkan_lagi` |
| 6 | Produk dan cetakan milik toko lain ditolak | Diuji · `produk_dan_template_toko_lain_ditolak` |
| 7 | Hanya pemilik yang bisa memasang dan mengelola; kasir ditolak dengan pesan sopan | API diuji · `hanya_pemilik_yang_bisa_mengelola` · layar Android belum |
| 8 | Harga asli tidak boleh lebih rendah dari harga jual | Ada di kode |
| 9 | Setiap publikasi jualan tercatat di `audit_logs` | **Belum**. Penukaran kode, catat sisa, toko, dan anggota sudah tercatat; `ListingMitraController` belum |
| 10 | Mitra wajib menyatakan alergen secara sadar sebelum terbit: mencentang alergen, atau memilih "Tidak mengandung alergen umum" | Belum. Lihat PRD-04 |
| 11 | Mitra sungguhan bisa memasang tas dari cetakan dalam waktu kurang dari 2 menit | Belum diukur. Ukur saat uji prototipe dengan mitra |

## Kosong dan galat

- Belum ada cetakan: M09 langsung membuka formulir tas campur, dengan tawaran menyimpannya sebagai cetakan.
- Belum ada produk: M16 menampilkan penjelasan bahwa produk didaftarkan tim selama pilot.
- Galat 422: tampilkan di bawah field yang bersangkutan. Kandungan kosong harus menyebut alasannya ("wajib diisi supaya pembeli yang alergi bisa memeriksa").

## Di luar lingkup

- Unggah foto jualan (F-20, belum ada endpoint).
- Jadwal pasang otomatis setiap hari.
- Mengubah harga setelah terbit. Pesanan yang sudah ada tetap memakai harga salinannya.
