# Kontrak API Life of Foods

Status: **draf**, disusun dari kode yang sudah ada di `main` (`api/routes/api.php` dan controller-nya) dan dipetakan ke layar di `docs/figma/peta-layar.md`. Kalau kode dan dokumen ini berbeda, kode yang berlaku; perbaiki dokumen ini di PR yang sama dengan perubahan kodenya.

## 1. Aturan umum

### Alamat dan header

| Lingkungan | Base URL |
|---|---|
| Laptop (`php artisan serve`) | `http://127.0.0.1:8000/api` |
| Emulator Android | `http://10.0.2.2:8000/api` |
| HP fisik | URL ngrok + `/api` |

Setiap permintaan mengirim:

```
Accept: application/json
Content-Type: application/json          (untuk POST, PUT, PATCH, DELETE ber-body)
Authorization: Bearer <token>           (untuk endpoint bertanda Token)
ngrok-skip-browser-warning: 1           (selalu; tanpa ini ngrok membalas halaman HTML)
```

Token didapat dari `POST /auth/otp/verify` atau `POST /auth/google`, disimpan Android di SharedPreferences privat aplikasi (`SesiPengguna`, tanpa cadangan ke cloud), dan berlaku sampai logout atau akun dihapus.

### Format data

- **Waktu**: ISO 8601 dengan zona `Asia/Jakarta`, contoh `2026-09-18T19:00:00+07:00`. Tanggal saja `YYYY-MM-DD`, jam saja `HH:mm`.
- **Uang**: bilangan bulat rupiah, kolom berakhiran `_rupiah`. Tidak ada desimal.
- **Berat**: bilangan bulat gram, kolom berakhiran `_gram`.
- **Nomor HP**: boleh dikirim `08xx`, `+628xx`, `628xx`, atau `8xx`. Server selalu mengembalikan `628xxxxxxxxx`.
- **Hari**: `day_of_week` 0 = Minggu sampai 6 = Sabtu.

### Bentuk respons

Kebanyakan respons dibungkus `data`:

```json
{ "data": { ... } }
```

Pengecualian: respons auth (`token`, `is_new_user`, `user`) dan `GET /me` tidak dibungkus.

Daftar berhalaman memakai paginator Laravel. Android cukup membaca `data`, `current_page`, `last_page`, dan `next_page_url` (`null` berarti halaman terakhir):

```json
{
  "data": [ ... ],
  "current_page": 1, "last_page": 3, "per_page": 20, "total": 47,
  "next_page_url": "http://.../api/listings?page=2", "prev_page_url": null
}
```

### Galat

| Status | Arti | Bentuk body |
|---|---|---|
| 401 | Token tidak ada atau tidak sah | `{ "message": "Unauthenticated." }` |
| 403 | Peran tidak boleh, atau akun dinonaktifkan | `{ "message": "..." }` |
| 404 | Tidak ditemukan, termasuk milik orang lain | `{ "message": "..." }` |
| 409 | Status data tidak mengizinkan aksi ini | `{ "message": "..." }` |
| 422 | Validasi gagal | `{ "message": "...", "errors": { "field": ["..."] } }` |
| 429 | Terlalu sering | `{ "message": "..." }` |
| 503 | Fitur belum tersedia di server ini | `{ "message": "..." }` |

`message` selalu dalam Bahasa Indonesia dan boleh langsung ditampilkan di Snackbar. Untuk 422, tampilkan `errors.<field>[0]` di bawah input yang bersangkutan.

Data milik orang lain (pesanan, toko, notifikasi) sengaja dijawab 404, bukan 403, supaya id tidak bisa ditebak.

#### Kode galat tetap (`code`)

Galat yang perlu ditangani Android dengan cara khusus membawa `code`. Android memakai `code`, bukan isi `message`, karena teks pesan boleh diubah kapan saja. Galat lain tidak punya `code`.

| `code` | Status | Di mana | Yang dilakukan Android |
|---|---|---|---|
| `account_inactive` | 403 | semua rute ber-token, verifikasi OTP, login Google | Hapus sesi, kembali ke K01 |
| `wrong_role` | 403 | verifikasi OTP, login Google. Ada juga `registered_role` (`consumer` atau `partner`) | Arahkan ke halaman masuk peran yang benar |
| `partner_not_registered` | 403 | minta dan verifikasi OTP dengan `role=partner` | Tampilkan pesan, jangan buat akun |
| `active_order_limit` | 422 | `POST /orders`. `errors.items` tetap ada | Arahkan ke K15 Pesanan saya |

### Peran

- `consumer`: aplikasi konsumen (K01 sampai K23).
- `partner`: aplikasi mitra (M01 sampai M21). Di dalam satu toko, perannya `owner` (pemilik) atau `cashier` (kasir). Kasir hanya boleh mencatat sisa, melihat pesanan masuk, mencocokkan kode, dan melihat dashboard.

Semua endpoint bertanda Token juga menolak akun yang dinonaktifkan dengan 403.

---

## 2. Daftar endpoint

Kolom Auth: **-** tanpa token, **T** butuh token. Kolom Peran: **K** konsumen, **M** mitra (pemilik atau kasir), **P** pemilik toko saja.

| Method | Path | Auth | Peran | Layar | Batas |
|---|---|---|---|---|---|
| POST | `/auth/otp/request` | - | | K02, M01 | 5/menit |
| POST | `/auth/otp/verify` | - | | K03, M02 | 10/menit |
| POST | `/auth/google` | - | | K02, M01 | 10/menit |
| POST | `/auth/logout` | T | K, M | K20, M15 | |
| GET | `/me` | T | K, M | K18, pembuka aplikasi | |
| PATCH | `/me` | T | K, M | K04, K19, K20 | |
| DELETE | `/me` | T | K | K20 | 3/menit |
| POST | `/me/photo` | T | K, M | K04, K19 | 10/menit |
| DELETE | `/me/photo` | T | K, M | K19 | |
| GET | `/allergens` | - | | K05, K09 | |
| PUT | `/me/allergens` | T | K | K05, K19 | |
| GET | `/listings` | - | | K07, K08, K09 | 60/menit |
| GET | `/listings/{id}` | - | | K10, K11 | 60/menit |
| GET | `/favorites` | T | K | K16 | |
| POST | `/favorites` | T | K | K10, K16 | 30/menit |
| DELETE | `/favorites/{store}` | T | K | K16 | |
| POST | `/orders/preview` | T | K | K12, K13 | 60/menit |
| POST | `/orders` | T | K | K12, K13 | 10/menit |
| GET | `/orders` | T | K | K15 | |
| GET | `/orders/{id}` | T | K | K14 | |
| POST | `/orders/{id}/cancel` | T | K | K14 | |
| POST | `/orders/{id}/review` | T | K | K15 | 20/menit |
| GET | `/notifications` | T | K, M | K17 | |
| POST | `/notifications/{id}/read` | T | K, M | K17 | |
| POST | `/notifications/read-all` | T | K, M | K17 | |
| GET | `/partner/stores` | T | M | setelah M02 | |
| GET | `/partner/stores/{store}` | T | M | M14 | |
| PUT, PATCH | `/partner/stores/{store}` | T | P | M14 | 30/menit |
| POST | `/partner/stores/{store}/photo` | T | P | M14 | 10/menit |
| POST | `/partner/stores/{store}/templates/{template}/photo` | T | P | M09 | 10/menit |
| POST | `/partner/stores/{store}/listings/{listing}/photo` | T | P | M09, M10 | 10/menit |
| GET | `/partner/stores/{store}/balance` | T | P | M13 | |
| GET | `/partner/stores/{store}/balance/transactions` | T | P | M13 | |
| GET | `/partner/stores/{store}/reviews` | T | M | M14 | |
| GET | `/partner/stores/{store}/members` | T | P | M15 | |
| POST | `/partner/stores/{store}/members` | T | P | M15 | 10/menit |
| DELETE | `/partner/stores/{store}/members/{member}` | T | P | M15 | |
| GET | `/partner/stores/{store}/templates` | T | P | M09 | |
| GET | `/partner/stores/{store}/products` | T | P | M16 | |
| PATCH | `/partner/stores/{store}/products/{product}` | T | P | M08 | |
| GET | `/partner/stores/{store}/listings` | T | P | M10, M17 | |
| POST | `/partner/stores/{store}/listings` | T | P | M09, M16 | 30/menit |
| POST | `/partner/stores/{store}/listings/{listing}/publish` | T | P | M10, M17 | |
| POST | `/partner/stores/{store}/listings/{listing}/pause` | T | P | M10, M17 | |
| GET | `/partner/stores/{store}/orders` | T | M | M11, M21 | |
| POST | `/pickup-codes/redeem` | T | M | M12, M18 | 20/menit |
| GET | `/partner/stores/{store}/waste-logs` | T | M | M06, M19 | |
| POST | `/partner/stores/{store}/waste-logs` | T | M | M06, M19 | 30/menit |
| GET | `/partner/stores/{store}/reports/weekly` | T | P | M07 | |
| GET | `/partner/stores/{store}/summary` | T | M | M05 | |
| GET | `/partner/stores/{store}/suggestions` | T | P | M08 | |
| POST | `/partner/stores/{store}/suggestions/{id}/accept` | T | P | M08 | |
| POST | `/partner/stores/{store}/suggestions/{id}/dismiss` | T | P | M08 | |

---

## 3. Auth dan profil

### POST /auth/otp/request

```json
{ "phone": "081234567890", "role": "consumer" }
```

`role`: `consumer` atau `partner`.

**202**

```json
{
  "message": "Mode uji coba: kode ditampilkan di layar, tidak dikirim lewat WhatsApp.",
  "expires_in": 300,
  "resend_in": 60,
  "pilot_code": "482913"
}
```

- `pilot_code` hanya ada selama `PILOT_MODE=true`. K03 menampilkannya dengan spanduk "mode uji coba".
- 403: nomor mitra belum terdaftar (akun mitra hanya dibuat tim lewat SeederPilot atau diundang pemilik toko).
- 429: diminta lagi sebelum `resend_in` detik.
- 503: `PILOT_MODE=false`, karena gateway WhatsApp belum ada.
- Meminta kode baru membatalkan kode sebelumnya.

### POST /auth/otp/verify

```json
{ "phone": "081234567890", "code": "482913", "role": "consumer" }
```

**201** (konsumen baru) atau **200** (masuk ulang):

```json
{
  "token": "12|pXkq...",
  "is_new_user": true,
  "user": { "id": 12, "name": null, "email": null, "phone": "6281234567890", "role": "consumer" }
}
```

- `is_new_user = true` berarti buka K04 Lengkapi profil, bukan beranda.
- 422: kode salah (pesan menyebut sisa percobaan), kedaluwarsa, atau belum diminta. Kode berlaku 5 menit, maksimal 5 kali salah.
- 429: percobaan habis, minta kode baru.
- 403: nomor terdaftar dengan peran lain ("Masuk lewat halaman mitra"), atau akun dinonaktifkan.

### POST /auth/google

```json
{ "id_token": "<ID token dari Credential Manager>", "role": "consumer" }
```

Respons sama dengan `/auth/otp/verify`. Konsumen baru dibuatkan akun. Mitra hanya bisa masuk kalau email Google-nya sama dengan email akun mitra yang sudah terdaftar; login Google tidak pernah membuat akun mitra.

- 401: token Google tidak sah.
- 403: email mitra belum terdaftar, peran tidak cocok, atau akun dinonaktifkan.
- 503: `GOOGLE_CLIENT_ID` belum diisi di server, atau server Google sedang tidak bisa dihubungi.

### POST /auth/logout

**204**. Hanya mencabut token perangkat ini.

### GET /me

```json
{
  "user": { "id": 12, "name": "Dara Renata", "email": null, "phone": "6281234567890", "role": "consumer", "has_google": false },
  "is_profile_complete": true,
  "consumer_profile": {
    "area_label": "Tebet, Jakarta Selatan", "latitude": -6.2267, "longitude": 106.8538,
    "notify_favorite_store": true, "notify_pickup_reminder": true, "notify_promo": false
  },
  "allergens": [ { "code": "kacang_tanah", "name": "Kacang tanah", "type": "allergen", "severity": "severe" } ]
}
```

- Dipanggil saat aplikasi dibuka. 401 berarti token tidak berlaku lagi; hapus token dan buka K01.
- `is_profile_complete = false` (nama masih kosong) berarti buka K04.
- Untuk mitra, `consumer_profile` bernilai `null` dan `allergens` kosong.

### PATCH /me

Semua field opsional; hanya yang dikirim yang diubah. Respons sama dengan `GET /me`.

| Field | Aturan | Peran |
|---|---|---|
| `name` | 2 sampai 120 karakter | K, M |
| `email` | email, unik, boleh `null` | K, M |
| `area_label` | maks 120 karakter | K |
| `latitude`, `longitude` | harus dikirim berpasangan | K |
| `notify_favorite_store`, `notify_pickup_reminder`, `notify_promo` | boolean | K |

Field khusus konsumen yang dikirim akun mitra ditolak 422. Contoh K20: `{ "notify_promo": true }`.

### Unggah foto (`POST /me/photo` dan tiga rute `.../photo` mitra)

Kirim sebagai `multipart/form-data` dengan field `photo`. Header `Accept: application/json` tetap wajib.

- Format JPEG, PNG, atau WebP. Ukuran maks 5 MB, sisi minimal 200 px dan maksimal 4096 px. Di luar batas ini ditolak 422 di `errors.photo`. Kamera HP 50 MP menghasilkan foto di atas 4096 px, jadi **Android wajib memperkecil foto sebelum mengunggah**, misalnya ke sisi terpanjang 1600 px.
- Server menyimpan ulang foto sebagai JPEG dengan sisi maksimal 1600 px. Metadata kamera, termasuk lokasi GPS, ikut terbuang. Foto yang miring karena EXIF diluruskan lebih dulu.
- **200** `{ "photo_url": "https://.../storage/listings/12/<uuid>.jpg" }`. Unggahan baru menggantikan foto lama, dan berkas lamanya dihapus.
- Hak akses: `/me/photo` untuk semua peran. Foto toko, template, dan jualan hanya untuk pemilik (kasir 403). Toko, template, atau jualan milik orang lain dijawab 404.
- **Foto template** (M09) dipakai lagi setiap kali tas dari template itu dipasang. **Foto jualan** hanya berlaku untuk satu listing, dan tidak menghapus foto template yang sebelumnya dipakai listing itu.
- `DELETE /me/photo` menghapus foto profil. Foto profil juga ikut terhapus saat akun dihapus (K20).
- `photo_url` ada di `GET /me` (`user.photo_url`), `GET /partner/stores/{store}`, `GET .../templates`, `GET .../listings` milik mitra, `GET /favorites`, dan listing konsumen.
- Di laptop dev, URL `/storage/...` baru bisa dibuka setelah `php artisan storage:link` dijalankan sekali.

### DELETE /me

```json
{ "confirm": true }
```

**204**. Hanya konsumen (mitra 403). Tampilkan dialog konfirmasi dulu.

- 409: masih ada pesanan yang belum diambil.
- Akun tanpa riwayat pesanan dihapus penuh. Akun yang pernah memesan dianonimkan (nomor HP, nama, email, catatan pesanan dikosongkan) supaya laporan mitra tetap utuh. Setelah itu nomor yang sama bisa mendaftar lagi.

### GET /allergens

```json
{ "data": [ { "code": "kacang_tanah", "name": "Kacang tanah", "type": "allergen" }, { "code": "vegan", "name": "Vegan", "type": "diet" } ] }
```

Urut sesuai `sort_order`. Chip K05 dan filter K09 dibangun dari sini, bukan di-hardcode.

### PUT /me/allergens

```json
{ "allergens": [ { "code": "kacang_tanah", "severity": "severe" }, { "code": "susu" } ] }
```

- Mengganti seluruh pilihan. `[]` mengosongkan. `severity`: `avoid` (bawaan) atau `severe`.
- Tombol "Lewati" di K05 tidak memanggil endpoint ini.
- **200** `{ "data": [ ...seperti allergens di /me... ] }`. Mitra 403. Kode tidak dikenal, dobel, atau nonaktif 422.

---

## 4. Listing (konsumen)

### GET /listings

Hanya jualan yang bisa dibeli sekarang: aktif, stok ada, jam ambil belum lewat, toko tidak tutup sementara. 20 per halaman.

| Query | Keterangan |
|---|---|
| `type[]` | `surprise_bag`, `menu_item` |
| `category` | `cafe`, `bakery`, `resto`, `catering`, `grocery` |
| `store_id` | jualan satu toko saja (K10 "Lihat menu satuan", K11) |
| `q` | cari di judul jualan dan nama toko |
| `lat`, `lng` | urut dari terdekat dan mengisi `distance_km` |
| `radius_km` | butuh `lat`/`lng`, 0,1 sampai 50 |
| `exclude_allergens[]` | kode alergen; jualan yang mengandung atau "mungkin mengandung" ikut disembunyikan |
| `halal=1` | label `certified` atau `self_claim` |
| `pickup_from`, `pickup_until` | `HH:mm`, rentang jam ambil beririsan |
| `ends_within_minutes` | untuk "Tutup kurang dari satu jam" |
| `sort=popular` | "Populer hari ini" di K07: hanya jualan dengan tanggal ambil hari ini yang sudah dipesan, urut dari `qty_ordered` terbanyak. Murni dari pesanan, bukan iklan. Iklan berbayar nanti tampil di tempat lain dengan label sendiri |
| `per_page` | 1 sampai 50 |

Satu item:

```json
{
  "id": 31, "type": "surprise_bag", "title": "Tas Pastry Sore", "photo_url": null,
  "price_rupiah": 18000, "original_value_rupiah": 55000, "qty_remaining": 3, "qty_ordered": 2,
  "pickup_start": "2026-09-18T19:00:00+07:00", "pickup_end": "2026-09-18T21:00:00+07:00",
  "minutes_until_end": 95, "halal_label": "self_claim",
  "allergens": [ { "code": "gluten", "name": "Gluten", "presence": "contains" } ],
  "distance_km": 0.38,
  "store": { "id": 5, "name": "Kopi Kalyan", "category": "cafe", "rating_average": 4.8, "rating_count": 180 }
}
```

`halal_label` ditampilkan apa adanya: `certified` menyebut nomor sertifikat, `self_claim` = "klaim mitra, belum bersertifikat", `not_stated` = "tidak disebutkan".

### GET /listings/{id}

Semua field ringkas di atas, ditambah:

```json
{
  "description": "...", "content_hint": "2-3 pastry campur", "ingredients_text": "Tepung, mentega, telur",
  "halal_certificate_no": null, "status": "active", "is_available": true,
  "items": [ { "label": "Croissant", "qty": 2, "weight_gram": null, "unit_value_rupiah": 28000 } ],
  "store": {
    "id": 5, "name": "Kopi Kalyan", "category": "cafe", "address": "Jl. ...", "latitude": -6.22, "longitude": 106.80,
    "hours_today": { "open_time": "07:00:00", "close_time": "21:00:00", "is_closed": 0 },
    "rating_average": 4.8, "rating_count": 180
  }
}
```

Jualan yang sudah habis atau lewat tetap bisa dibuka, dengan `is_available = false`; matikan tombol pesan. Draf dijawab 404.

---

## 5. Favorit (K16)

### GET /favorites?lat=&lng=

```json
{ "data": [ {
  "id": 5, "name": "Kopi Kalyan", "category": "cafe", "photo_path": null, "distance_km": 0.38,
  "closes_at": "21:00", "is_temporarily_closed": false,
  "available_bags": 2, "has_menu_available": false, "usual_publish_time": "19:00"
} ] }
```

Chip di kartu: `available_bags > 0` = "2 tas tersedia"; `has_menu_available` = "Menu satuan tersedia"; `usual_publish_time` = "Biasanya pasang jam 19.00"; selain itu "Belum ada tas hari ini". Tab "Tas" belum didukung.

### POST /favorites

`{ "store_id": 5 }`. **201** kalau baru, **200** kalau sudah ada: `{ "data": { "store_id": 5, "is_favorite": true } }`. Toko tidak ada 404.

### DELETE /favorites/{store}

**204**, juga kalau toko itu memang belum difavoritkan.

---

## 6. Pesanan (konsumen)

Bayar di tempat selama pilot (ADR-0004): tidak ada langkah pembayaran di aplikasi.

### POST /orders/preview

```json
{ "items": [ { "listing_id": 31, "qty": 1 } ] }
```

Tidak menyimpan apa pun.

```json
{ "data": {
  "store": { "id": 5, "name": "Kopi Kalyan", "address": "Jl. ..." },
  "items": [ { "listing_id": 31, "title": "Tas Pastry Sore", "qty": 1, "unit_price_rupiah": 18000, "line_total_rupiah": 18000 } ],
  "pickup_start": "2026-09-18T19:00:00+07:00", "pickup_end": "2026-09-18T21:00:00+07:00",
  "subtotal_rupiah": 18000, "service_fee_rupiah": 0, "discount_rupiah": 0, "total_rupiah": 18000,
  "allergen_warnings": [ { "listing_id": 31, "code": "susu", "name": "Susu", "presence": "may_contain", "severity": "avoid" } ]
} }
```

`allergen_warnings` tidak memblokir pesanan, tapi K12/K13 wajib menampilkannya sebelum tombol "Buat pesanan".

Aturan (422 dengan pesan di `errors.items` atau `errors.items.N.listing_id`):

- satu pesanan untuk satu toko, dan jam ambil semua item harus beririsan;
- maksimal 5 per item dan 10 item per pesanan;
- maksimal 3 pesanan yang belum diambil per pembeli;
- stok tidak cukup atau jualan sudah tidak tersedia.

### POST /orders

Body sama dengan preview, ditambah `note` (opsional, maks 300) dan `payment_method` (`cash` bawaan, atau `qris_static`). **201** berisi detail pesanan seperti `GET /orders/{id}`.

### GET /orders?status=active|history

20 per halaman: `id`, `code`, `status`, `store_name`, `item_count`, `total_rupiah`, `pickup_start`, `pickup_end`, `placed_at`, `review_rating` (bintang yang sudah diberi, atau `null`), `can_review`.

### GET /orders/{id}

```json
{ "data": {
  "id": 88, "code": "LOF-7Q2K9A", "status": "pending_pickup",
  "pickup_code": "LF7Q2K", "pickup_code_status": "active",
  "pickup_start": "...", "pickup_end": "...",
  "store": { "id": 5, "name": "Kopi Kalyan", "address": "Jl. ...", "latitude": -6.22, "longitude": 106.80 },
  "items": [ { "listing_id": 31, "title": "Tas Pastry Sore", "unit_price_rupiah": 18000, "qty": 1, "line_total_rupiah": 18000 } ],
  "item_count": 1, "subtotal_rupiah": 18000, "service_fee_rupiah": 0, "discount_rupiah": 0, "total_rupiah": 18000,
  "payment_method": "cash", "payment_status": "unpaid", "note": null,
  "placed_at": "...", "completed_at": null, "cancelled_at": null,
  "review": null, "can_review": false
} }
```

- `status`: `pending_pickup`, `completed`, `cancelled`, `no_show`.
- `pickup_code` hanya terisi selama kodenya masih bisa ditukar (K14).
- Pesanan yang tidak diambil sampai 30 menit setelah `pickup_end` otomatis jadi `no_show`.
- `review`: `{ "rating": 5, "comment": "...", "created_at": "..." }` atau `null`. `can_review` bernilai `true` untuk pesanan `completed` sampai 7 hari setelah `completed_at`.

### POST /orders/{id}/review

`{ "rating": 5, "comment": "Croissantnya masih renyah" }`. `rating` wajib, 1 sampai 5. `comment` opsional, maks 500.

- **201** untuk ulasan baru, **200** kalau menimpa ulasan lama pesanan yang sama. Isinya `{ "data": { "rating", "comment", "created_at", "updated_at" } }`.
- 409 kalau pesanan belum `completed` atau sudah lewat 7 hari sejak diambil. 404 untuk pesanan orang lain, 403 untuk akun mitra.
- Rating toko (`rating_average` satu angka di belakang koma, `rating_count`) ikut tampil di `store` pada `GET /listings`, `GET /listings/{id}`, dan `GET /partner/stores/{store}`. `rating_average` bernilai `null` kalau belum ada ulasan. Pada `sort=popular`, rating dipakai sebagai urutan kedua setelah jumlah pesanan.

### POST /orders/{id}/cancel

`{ "reason": "..." }` opsional. **200** detail pesanan. Stok kembali. 409 kalau statusnya bukan `pending_pickup`.

---

## 7. Notifikasi (K17)

Hanya di dalam aplikasi; ambil saat layar dibuka atau aplikasi kembali ke depan. Push (FCM) belum ada.

### GET /notifications?unread=1

20 per halaman, ditambah `unread_count` di tingkat atas untuk lencana lonceng.

```json
{
  "data": [ {
    "id": 301, "type": "pengingat_ambil", "title": "Pesananmu siap diambil",
    "body": "Tas Pastry Sore di Kopi Kalyan. Tunjukkan kode LF7Q2K ke kasir.",
    "data": { "screen": "K14", "order_id": 88 }, "is_read": false, "created_at": "..."
  } ],
  "unread_count": 3, "current_page": 1, "last_page": 1
}
```

| `type` | Penerima | Kapan | Ketukan membuka |
|---|---|---|---|
| `pesanan_baru` | pemilik dan kasir | pesanan dibuat | M11 (`store_id`, `order_id`) |
| `pengingat_ambil` | pembeli, kalau `notify_pickup_reminder` | 30 menit sebelum jam ambil | K14 (`order_id`) |
| `porsi_terselamatkan` | pembeli | kode ditukar | K14 (`order_id`) |
| `mitra_favorit_memasang` | yang memfavoritkan toko, kalau `notify_favorite_store` | jualan terbit, maks sekali per toko per hari | K10 (`listing_id`) |

### POST /notifications/{id}/read, POST /notifications/read-all

**204**. Notifikasi orang lain 404.

---

## 8. Toko (mitra)

### GET /partner/stores

Dipanggil setelah M02 untuk mendapatkan `store_id` dan peran.

```json
{ "data": [ { "id": 5, "name": "Kopi Kalyan SCBD", "category": "cafe", "address": "...", "photo_path": null, "is_temporarily_closed": false, "my_role": "owner" } ] }
```

### GET /partner/stores/{store}

```json
{ "data": {
  "id": 5, "name": "Kopi Kalyan SCBD", "slug": "kopi-kalyan", "category": "cafe", "address": "Jl. Jend. Sudirman Kav 52",
  "latitude": -6.22, "longitude": 106.80, "whatsapp": "6281299887766", "photo_path": null,
  "halal_label": "self_claim", "halal_certificate_no": null, "default_ingredients_text": "...",
  "is_temporarily_closed": false, "is_pilot_partner": true, "rating_average": 4.8, "rating_count": 180,
  "hours": [ { "day_of_week": 1, "is_closed": false, "open_time": "07:00", "close_time": "21:00" } ],
  "my_role": "owner", "available_balance_rupiah": 1186000
} }
```

Untuk kasir, `available_balance_rupiah` bernilai `null`.

### PUT atau PATCH /partner/stores/{store}

Semua field opsional. Respons sama dengan GET.

| Field | Aturan |
|---|---|
| `name` | 2 sampai 140 |
| `category` | lihat kategori di atas |
| `address` | 5 sampai 255 |
| `latitude`, `longitude` | berpasangan |
| `whatsapp` | nomor seluler, dinormalisasi |
| `halal_label` | `certified` wajib disertai `halal_certificate_no` |
| `halal_certificate_no`, `default_ingredients_text` | teks |
| `is_temporarily_closed` | sakelar "Tutup sementara"; jualan disembunyikan dari konsumen |
| `hours[]` | `{ day_of_week, open_time, close_time }` atau `{ day_of_week, is_closed: true }`; tutup harus setelah buka |

### GET /partner/stores/{store}/balance

```json
{ "data": {
  "available_rupiah": 1186000, "pending_rupiah": 18000, "lifetime_rupiah": 2086000, "items_sold_this_week": 62,
  "withdrawal": { "enabled": false, "reason": "Pencairan tersedia setelah masa uji coba." }
} }
```

Tombol "Cairkan" ditampilkan nonaktif dengan `withdrawal.reason`.

### GET /partner/stores/{store}/balance/transactions

20 per halaman: `id`, `type` (`sale`, `adjustment`, `withdrawal`, `service_fee`), `amount_rupiah` (negatif untuk pengurangan), `balance_after_rupiah`, `title` (contoh "Tas Pastry Sore" atau "Tas Pastry Sore +1 lainnya"), `pickup_code`, `description`, `created_at`.

### GET /partner/stores/{store}/reviews

Pemilik dan kasir. 20 per halaman, terbaru dulu: `id`, `rating`, `comment`, `buyer_name` (nama depan saja, "Pembeli" untuk akun yang sudah dihapus), `created_at`. Di luar paginasi ada `summary`: `{ "rating_average": 4.8, "rating_count": 180 }`.

### GET /partner/stores/{store}/members

Baris pertama selalu pemilik toko (`is_store_owner: true`, `id: null`).

```json
{ "data": [
  { "id": null, "user_id": 3, "name": "Kalyan Pratama", "phone": "6281299887766", "role": "owner", "is_store_owner": true, "invited_at": null },
  { "id": 14, "user_id": 9, "name": "Rina", "phone": "6281322445566", "role": "cashier", "is_store_owner": false, "invited_at": "..." }
] }
```

### POST /partner/stores/{store}/members

```json
{ "phone": "081311112222", "name": "Budi" }
```

**201** berisi baris anggota ditambah `invite_message`, teks untuk dibagikan lewat intent WhatsApp. Kasir lalu masuk lewat OTP di M01 dengan nomor itu. 409 kalau nomornya milik akun konsumen, sudah jadi anggota, atau milik pemilik toko.

### DELETE /partner/stores/{store}/members/{member}

**204**. Akses kasir ke toko ini langsung hilang. Pemilik tidak bisa dicabut (409).

---

## 9. Jualan (mitra)

### GET /partner/stores/{store}/templates, GET /partner/stores/{store}/products

Template tas (M09): `id`, `name`, `content_hint`, `price_rupiah`, `original_value_rupiah`, `default_qty`, `pickup_start_time`, `pickup_end_time`, `halal_label`.

Produk (M16): `id`, `name`, `unit`, `price_rupiah`, `ingredients_text`.

### PATCH /partner/stores/{store}/products/{product}

`{ "daily_production_qty": 24 }` atau `null` untuk mengosongkan. Dipakai saran produksi M08.

### POST /partner/stores/{store}/listings

Tas kejutan (M09):

```json
{ "type": "surprise_bag", "template_id": 2, "qty_total": 5, "pickup_start": "19:00", "pickup_end": "21:00" }
```

Tanpa template ("tas campur"), wajib `title` dan `price_rupiah`. Opsional: `description`, `content_hint`, `original_value_rupiah` (tidak boleh di bawah harga), `ingredients_text`, `halal_label`, `allergens[]` (`{ code, presence: contains|may_contain }`).

Menu satuan (M16), satu item menjadi satu listing:

```json
{ "type": "menu_item", "pickup_start": "19:00", "pickup_end": "21:00",
  "items": [ { "product_id": 7, "qty_total": 4, "price_rupiah": 14000, "allergens": [ { "code": "susu" } ] } ] }
```

- `publish` bawaan `true`. Kirim `false` untuk menyimpan draf.
- Jualan tidak bisa terbit kalau kandungannya kosong atau jam ambilnya sudah lewat (422).
- **201** `{ "data": [ ...listing mitra... ] }`.

Bentuk listing mitra: `id`, `type`, `status`, `title`, `product_id`, `template_id`, `price_rupiah`, `original_value_rupiah`, `qty_total`, `qty_reserved`, `qty_sold`, `qty_remaining`, `potential_income_rupiah`, `pickup_start`, `pickup_end`, `ingredients_text`, `halal_label`, `published_at`, `allergens[]`.

### GET /partner/stores/{store}/listings?type=&status=&date=

Jualan pada tanggal itu (bawaan hari ini). `status`: `draft`, `active`, `paused`, `sold_out`, `expired`.

### POST .../listings/{listing}/publish, POST .../listings/{listing}/pause

**200** satu listing. Publish dari `draft` atau `paused`; pause dari `active`. Status lain 409. Pause hanya menghentikan pesanan baru.

---

## 10. Pesanan masuk dan kode pickup (mitra)

### GET /partner/stores/{store}/orders?status=pending|history&date=

30 per halaman.

```json
{
  "id": 88, "code": "LOF-7Q2K9A", "status": "pending_pickup", "buyer_name": "Dara",
  "items": [ { "title": "Tas Pastry Sore", "qty": 1, "line_total_rupiah": 18000 } ], "item_count": 1,
  "total_rupiah": 18000, "payment_method": "cash", "payment_status": "unpaid",
  "note": "Alergi kacang, tolong dipisah ya",
  "allergen_snapshot": [ { "code": "kacang_tanah", "name": "Kacang tanah", "severity": "severe" } ],
  "pickup_start": "...", "pickup_end": "...", "placed_at": "...", "completed_at": null
}
```

Kode pickup tidak ikut; kasir mengetik atau memindai kode dari HP pembeli.

### POST /pickup-codes/redeem

```json
{ "store_id": 5, "code": "LF7Q2K" }
```

Huruf kecil diterima. **200** satu pesanan berbentuk seperti di atas dengan `status: completed` dan `payment_status: paid`.

- 404: kode tidak ada di toko ini.
- 409: sudah dipakai (pesan menyebut jamnya), pesanan dibatalkan, atau lebih dari 30 menit setelah jam ambil.

M18 Pindai QR memakai endpoint yang sama; QR cukup berisi kodenya.

---

## 11. Catat sisa, laporan, dan dashboard (mitra)

### GET /partner/stores/{store}/waste-logs?date=

```json
{ "data": {
  "log_date": "2026-09-18", "is_recorded": true, "is_locked": false, "method": "per_item", "note": null,
  "total_value_rupiah": 42000, "total_weight_gram": 0, "total_items": 6, "change_vs_last_week_percent": 12,
  "products": [ { "product_id": 7, "name": "Croissant", "unit": "pcs", "price_rupiah": 28000, "unit_value_rupiah": 7000,
                  "qty": 6, "weight_gram": null, "disposition": "discarded" } ],
  "other_items": [ { "label": "Roti tawar sisa", "qty": 2, "weight_gram": null, "value_rupiah": 6000 } ]
} }
```

`products` berisi semua produk aktif, termasuk yang sisanya 0, supaya layar cukup tombol +/-.

### POST /partner/stores/{store}/waste-logs

```json
{ "log_date": "2026-09-18", "method": "per_item", "note": null,
  "items": [ { "product_id": 7, "qty": 6, "disposition": "discarded" }, { "label": "Roti tawar sisa", "qty": 2, "unit_value_rupiah": 3000 } ] }
```

- `method`: `per_item` (wajib `qty`) atau `weight` (M19, wajib `weight_gram`).
- `disposition`: `discarded` (bawaan), `donated`, `staff_meal`, `sold_surplus`. Hanya `discarded` dihitung pemborosan.
- Satu catatan per toko per hari; kirim ulang mengganti isinya. Nilai memakai HPP produk, harga jual kalau HPP kosong.
- 409 setelah akhir hari berikutnya (terkunci). 422 untuk tanggal yang belum terjadi.

### GET /partner/stores/{store}/reports/weekly?week_start=

`week_start` adalah Senin; bawaan minggu ini.

```json
{ "data": {
  "week_start": "2026-09-14", "week_end": "2026-09-20",
  "wasted_value_rupiah": 210000, "wasted_weight_gram": 0, "logged_days": 5,
  "rescued_value_rupiah": 1186000, "orders_count": 58, "items_sold": 62,
  "unsold_value_rupiah": 1396000, "wasted_change_percent": -8,
  "top_wasted_products": [ { "product_id": 7, "label": "Croissant", "qty": 18, "weight_gram": null, "value_rupiah": 126000 } ],
  "daily": [ { "date": "2026-09-14", "wasted_value_rupiah": 42000 }, { "date": "2026-09-15", "wasted_value_rupiah": null } ]
} }
```

Di `daily`, `null` berarti hari itu tidak dicatat (bukan 0). Gambar grafik dengan celah.

### GET /partner/stores/{store}/summary

```json
{ "data": {
  "store": { "id": 5, "name": "Kopi Kalyan SCBD" },
  "is_open": true, "closes_at": "21:00", "minutes_until_close": 120, "is_today_logged": false,
  "rescued_value_this_week_rupiah": 1186000, "items_sold_this_week": 62,
  "avg_daily_waste_gram": 850, "unsold_bags_last_7_days": 4, "pending_orders_today": 3,
  "daily": [ { "date": "2026-09-14", "wasted_value_rupiah": 42000 } ], "peak_day": "Sabtu"
} }
```

`is_today_logged = false` menampilkan pengingat catat sisa.

### GET /partner/stores/{store}/suggestions?date=

`date` harus setelah hari ini; bawaan besok.

```json
{ "data": {
  "suggested_for_date": "2026-09-19", "weekday": "Sabtu", "sample_days": 3, "has_enough_data": true,
  "products_missing_production_qty": 2, "total_saving_per_week_rupiah": 112000,
  "items": [ { "id": 40, "product_id": 7, "name": "Croissant mentega", "unit": "pcs",
               "current_production": 24, "suggested_production": 20, "reduce_by": 4, "avg_waste_qty": 6.0,
               "estimated_saving_per_week_rupiah": 112000,
               "rationale": "Rata-rata sisa 6 setiap Sabtu dari 3 catatan terakhir.", "status": "pending" } ]
} }
```

`has_enough_data = false` (kurang dari 3 catatan) berarti tampilkan ajakan mencatat sisa. `products_missing_production_qty` adalah jumlah produk yang belum diisi `daily_production_qty`.

### POST .../suggestions/{id}/accept, POST .../suggestions/{id}/dismiss

**200** `{ "data": { "id": 40, "status": "accepted" } }`.

---

## 12. Belum ada di API

| Layar | Hal | Alasan |
|---|---|---|
| K05 | Chip "Halal saja" dan "Tidak pedas" | Halal lewat filter `halal=1` di listing; tingkat pedas belum ada di skema |
| K09 | Filter vegetarian | Belum ada penanda vegetarian per jualan |
| K16 | Tab Tas | `favorites` hanya menyimpan toko |
| K17 | Push notification | FCM belum dipasang |
| K21, K22 | Voucher | WON'T selama pilot |
| M13, M20 | Pencairan saldo | WON'T selama pilot |
| M14 | Lencana terverifikasi, label alergen bawaan toko | Belum ada di skema |
| M15 | Sakelar pengingat, rekening pencairan, dokumen verifikasi | Belum ada di skema |
