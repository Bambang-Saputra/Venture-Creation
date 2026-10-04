# Peta layar Figma → fitur, endpoint, dan tabel

Sumber desain: file Figma **Life of Foods v2** (`ru6anFK6NP9chxuSErWOFX`), halaman **Layar aplikasi**.
44 layar: 23 konsumen (K01–K23) dan 21 mitra (M01–M21), tersusun dalam 6 section.

Di Figma, setiap elemen yang bisa diklik memuat tujuannya di nama layer, misalnya `Pesan →K12` dan `Kembali →BACK`. Konvensi itu dipakai saat menyambungkan prototipe, dan tetap dipakai kalau menambah layar baru.

Dokumen ini adalah rujukan tunggal saat menulis PRD, endpoint, dan layar Android. Kalau ada perbedaan antara dokumen ini dan Figma, perbaiki keduanya dalam PR yang sama.

## Cara membaca kolom prioritas

| Nilai | Arti |
|---|---|
| **MUST** | Harus jalan saat uji coba mitra dan BIFEST minggu 9 |
| SHOULD | Sangat diinginkan sebelum BIFEST, boleh dibuang kalau jalur kritis terancam |
| COULD | Dikerjakan hanya kalau waktu tersisa |
| WON'T | Tidak dikerjakan siklus ini. Layarnya tetap ada di Figma dan ditunjukkan saat presentasi sebagai peta jalan produk |

---

## Konsumen

| Kode | Layar | Fitur | Prioritas | Endpoint | Tabel |
|---|---|---|---|---|---|
| K01 | Pilih peran | F-01 | MUST | — | — |
| K02 | Masuk | F-01 | MUST | `POST /auth/otp/request` | `users`, `otp_codes` |
| K02 | Masuk (tombol Google) | F-25 | SHOULD | `POST /auth/google` | `users`, `personal_access_tokens` |
| K03 | Verifikasi OTP | F-01 | MUST | `POST /auth/otp/verify` | `users`, `otp_codes`, `personal_access_tokens` |
| K04 | Lengkapi profil | F-02 | MUST | `PATCH /me` | `users`, `consumer_profiles` |
| K05 | Alergi dan pantangan | F-02 | MUST | `GET /allergens`, `PUT /me/allergens` | `allergens`, `user_allergens` |
| K06 | Onboarding | — | SHOULD | — | — |
| K07 | Beranda | F-03 | MUST | `GET /listings` | `listings`, `stores`, `listing_allergens` |
| K08 | Peta | F-14 | SHOULD | `GET /listings?lat&lng&radius_km` | `listings`, `stores` |
| K09 | Filter | F-04 | MUST | `GET /listings?exclude_allergens[]` | `listing_allergens`, `user_allergens` |
| K10 | Detail tas kejutan | F-05 | MUST | `GET /listings/{id}`, `GET /listings?store_id=&type[]=menu_item`, `POST /favorites` | `listings`, `listing_allergens`, `stores` |
| K11 | Detail menu satuan | F-05 | MUST | `GET /listings/{id}`, `GET /listings?store_id=&type[]=menu_item` | `listings`, `listing_items`, `products` |
| K12 | Ringkasan tas kejutan | F-06 | MUST | `POST /orders/preview`, `POST /orders` | `orders`, `order_items`, `pickup_codes` |
| K13 | Ringkasan menu satuan | F-06 | MUST | `POST /orders/preview`, `POST /orders` | `orders`, `order_items`, `pickup_codes` |
| K14 | Kode pickup | F-07 | MUST | `GET /orders/{id}` | `orders`, `pickup_codes` |
| K15 | Pesanan saya | F-08 | MUST | `GET /orders` | `orders`, `order_items` |
| K16 | Favorit | F-22 | COULD | `GET/POST/DELETE /favorites` | `favorites`, `stores` |
| K17 | Notifikasi | F-18 | SHOULD | `GET /notifications`, `POST /notifications/{id}/read`, `POST /notifications/read-all` | `notifications` |
| K18 | Profil | F-19 | SHOULD | `GET /me` | `users`, `consumer_profiles`, `orders` |
| K19 | Edit profil | F-19 | SHOULD | `PATCH /me` | `users`, `consumer_profiles` |
| K20 | Pengaturan | F-19 | SHOULD | `PATCH /me`, `DELETE /me` | `users` |
| K21 | Metode pembayaran | — | WON'T | — | — |
| K22 | Voucher saya | — | WON'T | — | — |
| K23 | Pusat bantuan | — | COULD | — | — |

Catatan layar konsumen:
- **K01** hanya memilih graf navigasi (`nav_konsumen.xml` atau `nav_mitra.xml`), tidak memanggil API.
- **K02 dan M01** punya tombol "Lanjut dengan akun Google" (F-25, lihat ADR-0006). Konsumen baru dari Google langsung ke K04 tanpa K03. Mitra hanya bisa masuk lewat Google kalau emailnya sudah terdaftar di akun mitra. Layer tombol itu di Figma masih bernama `Lanjut ke ringkasan` tanpa tujuan `→`; ganti namanya saat menyambungkan prototipe.
- **K03** saat `PILOT_MODE=true` menampilkan kode OTP di layar dengan spanduk "mode uji coba". Ini disengaja karena gateway WhatsApp berbayar, dan harus disebut terus terang saat demo.
- **K08** memakai osmdroid dengan ubin OpenStreetMap (ADR-0005, revisi 4 Oktober 2026), tanpa API key dan tanpa biaya. Penanda harga ditampilkan satu per toko, dan kartu jualan terpilih muncul di bawah peta. Tab Daftar berisi jualan berurut jarak dengan tombol yang membuka aplikasi peta lewat intent `geo:`. Kalau pembeli memberi izin lokasi (diminta di halaman terakhir K06), jarak dihitung dari posisi HP; kalau tidak, dari area di profil.
- **K10 dan K11** wajib menampilkan label kandungan dan status halal apa adanya: `certified` menyebut nomor sertifikat, `self_claim` berbunyi "klaim mitra, belum bersertifikat", `not_stated` berbunyi "tidak disebutkan". Aplikasi tidak pernah menulis "halal" tanpa dasar.
- **K12 dan K13** memakai endpoint yang sama; bedanya hanya isi keranjang, satu tas utuh atau daftar item.
- **K16** baru mendukung tab Mitra. Tab Tas belum ada karena `favorites` hanya menyimpan toko, sedangkan jualan berganti setiap hari.
- **K17** hanya notifikasi di dalam aplikasi: Android membaca `GET /notifications` saat layar dibuka. Push lewat FCM belum ada. Jenis yang dibuat server: `pesanan_baru` (ke pemilik dan kasir), `pengingat_ambil` (30 menit sebelum jam ambil), `porsi_terselamatkan` (setelah kode ditukar), `mitra_favorit_memasang` (paling banyak satu per toko per hari). Contoh "Cek kandungan" dan voucher di Figma belum dibuat.
- **K20** "Hapus akun" memanggil `DELETE /me` dengan `{"confirm": true}` setelah dialog konfirmasi. Ditolak 409 kalau masih ada pesanan yang belum diambil. Akun yang pernah memesan dianonimkan, bukan dihapus, supaya laporan mitra tetap utuh.
- **K21 dan K22** tampilan saja. Pembayaran saat pilot dilakukan tunai atau QRIS milik mitra di tempat, dan tabel voucher belum ada di skema versi pertama.

---

## Mitra

| Kode | Layar | Fitur | Prioritas | Endpoint | Tabel |
|---|---|---|---|---|---|
| M01 | Masuk mitra | F-01 | MUST | `POST /auth/otp/request` | `users` |
| M01 | Masuk mitra (tombol Google) | F-25 | SHOULD | `POST /auth/google` | `users`, `personal_access_tokens`, `store_members` |
| M02 | Verifikasi OTP mitra | F-01 | MUST | `POST /auth/otp/verify` | `users`, `personal_access_tokens`, `store_members` |
| M03 | Daftar usaha | — | WON'T | — | — |
| M04 | Verifikasi dan rekening | — | WON'T | — | — |
| M05 | Dashboard mitra | F-24 | SHOULD | `GET /partner/stores/{id}/summary` | `orders`, `waste_logs`, `weekly_reports` |
| M06 | Catat sisa | F-12 | MUST | `POST/GET /partner/stores/{id}/waste-logs` | `waste_logs`, `waste_log_items`, `products` |
| M07 | Laporan mingguan | F-13 | MUST | `GET /partner/stores/{id}/reports/weekly` | `weekly_reports`, `waste_logs`, `orders` |
| M08 | Saran produksi | F-15 | SHOULD | `GET /partner/stores/{id}/suggestions` | `production_suggestions`, `waste_log_items` |
| M09 | Pasang tas | F-09 | MUST | `POST .../listings`, `POST .../listings/{id}/publish` | `listings`, `surprise_bag_templates`, `listing_allergens`, `store_hours` |
| M10 | Kelola jualan | F-09 | MUST | `GET .../listings`, `POST .../listings/{id}/pause` | `listings` |
| M11 | Pesanan masuk | F-10 | MUST | `GET /partner/stores/{id}/orders` | `orders`, `order_items` |
| M12 | Cocokkan kode | F-11 | MUST | `POST /pickup-codes/redeem` | `pickup_codes`, `orders`, `listings`, `balance_transactions`, `audit_logs` |
| M13 | Saldo dan pencairan | F-17 | SHOULD | `GET /partner/stores/{id}/balance`, `GET .../balance/transactions` | `store_balances`, `balance_transactions` |
| M14 | Profil toko | F-16 | SHOULD | `GET /partner/stores`, `GET/PUT /partner/stores/{id}` | `stores`, `store_hours` |
| M15 | Pengaturan toko | F-16 | SHOULD | `GET/POST/DELETE .../members` | `store_members`, `audit_logs` |
| M16 | Pasang menu satuan | F-09 | MUST | `POST .../listings` tipe `menu` | `listings`, `listing_items`, `products` |
| M17 | Kelola menu satuan | F-09 | MUST | `GET .../listings?type=menu` | `listings`, `listing_items` |
| M18 | Pindai QR | F-11 | COULD | `POST /pickup-codes/redeem` | sama dengan M12 |
| M19 | Catat sisa timbang | F-12 | MUST | `POST .../waste-logs` metode `weight` | `waste_logs`, `waste_log_items` |
| M20 | Konfirmasi pencairan | — | WON'T | — | — |
| M21 | Riwayat pesanan | F-10 | SHOULD | `GET .../orders?status=history` | `orders`, `order_items` |

Catatan layar mitra:
- **M03 dan M04** tidak dibangun. Akun mitra dibuat tim lewat `SeederPilot` yang membaca CSV lokal, dan CSV itu tidak pernah masuk repo.
- **M06 dan M19** adalah satu fitur dengan dua mode: hitung per item dan timbang per kilogram. Nilai rupiah dihitung saat mengetik lalu **disimpan**, bukan dihitung ulang dari HPP yang bisa berubah.
- **M11** wajib menampilkan catatan konsumen dan alergi langsung di kartu daftar, bukan disembunyikan di detail. Menyimpan alergi tanpa menampilkannya sama saja tidak punya fitur alergi.
- **M12** adalah momen demo berlangsung. Kode sudah dipakai menghasilkan `409` beserta jam pemakaian, kode milik toko lain menghasilkan `403`, dan setiap penukaran masuk `audit_logs` lengkap dengan identitas kasir.
- **M18** memakai endpoint yang sama dengan M12, tapi butuh CameraX dan pemindai kode. Mode ketik kode sudah cukup untuk BIFEST, jadi ini COULD.
- **M13 dan M20**: tombol cairkan tampil tapi nonaktif dengan teks "tersedia setelah masa uji coba". Tim tidak memegang uang mitra selama pilot.
- **M15** menegakkan batas peran: kasir boleh mencatat sisa dan mencocokkan kode, tapi ditolak di saldo, tulis listing, dan kelola anggota. Penolakan ditampilkan sopan, bukan crash.

---

## Irisan vertikal minggu 5

Sembilan layar ini yang dikejar lebih dulu, karena sudah membentuk satu alur utuh yang bisa didemokan ke dosen:

`K01 → K02 → K03 → K07 → K10 → K12 → K14` di sisi konsumen, lalu `M11 → M12` di sisi mitra.

Layar lain boleh terlambat. Alur di atas tidak boleh.
