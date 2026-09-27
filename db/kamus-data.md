# Kamus data — aturan yang tidak terlihat dari nama kolom

Daftar kolom lengkap ada di `api/database/migrations/`. Dokumen ini hanya
memuat hal yang tidak bisa ditebak dari nama kolom, dan yang kalau dilanggar
menghasilkan bug yang baru ketahuan saat demo.

## Aturan umum

| Hal | Aturan |
|---|---|
| Uang | `INT UNSIGNED`, rupiah penuh, tanpa desimal. Nama kolom selalu berakhiran `_rupiah`. Satu-satunya kolom uang bertanda adalah `balance_transactions.amount_rupiah` |
| Berat | `INT UNSIGNED` dalam gram. Tampilkan sebagai kilogram di layar, simpan sebagai gram |
| Waktu | `*_at` untuk `TIMESTAMP`, `*_date` untuk `DATE`, `*_time` untuk `TIME` |
| Boolean | selalu berawalan `is_` |
| Nomor HP | ternormalisasi `628xxxxxxxxx`, tanpa tanda plus, tanpa spasi. `users.phone` boleh `NULL` untuk akun dari Google (ADR-0006), jadi kode yang mengirim WhatsApp wajib memeriksanya dulu |
| Identitas Google | `users.google_sub` berisi klaim `sub` ID token, bukan email. Unik, dan hanya diisi server di `POST /auth/google` |
| Produksi harian | `products.daily_production_qty`, jumlah yang biasa dibuat per hari, diisi mitra. `NULL` berarti produk itu tidak diberi saran produksi (M08) |
| Zona waktu | database menyimpan waktu apa adanya; aplikasi memakai `Asia/Jakarta` |

## Kolom salinan, dan kenapa ada

Beberapa nilai sengaja disalin, bukan dibaca lewat relasi. Ini bukan kelalaian
normalisasi, ini keputusan.

| Kolom | Disalin dari | Alasan |
|---|---|---|
| `listings.title`, `price_rupiah`, `ingredients_text`, `halal_label` | `products` atau `surprise_bag_templates` | Mitra boleh mengubah produknya besok. Jualan hari ini tidak boleh ikut berubah |
| `order_items.title_snapshot`, `unit_price_rupiah` | `listings` | Struk pembeli harus tetap menunjukkan angka yang dia lihat saat memesan |
| `orders.allergen_snapshot` | `user_allergens` | Mitra harus melihat pantangan pembeli seperti saat pesanan dibuat, walau profilnya berubah setelah itu |
| `waste_log_items.value_rupiah`, `unit_value_rupiah` | `products.cost_rupiah` | Kalau HPP naik bulan depan, laporan bulan lalu harus tetap sama. Laporan yang berubah sendiri tidak dipercaya, dan mitra berhenti mengisi |

## Yang ditegakkan database, bukan hanya aplikasi

Empat `CHECK` constraint dipasang di migrasi. Semuanya sudah diuji menolak
data yang salah, dan menerima data yang benar.

| Constraint | Isi | Kenapa di database |
|---|---|---|
| `chk_listings_kandungan` | `status = 'draft'` atau `ingredients_text` terisi minimal 3 karakter | Janji keamanan alergi tidak boleh bergantung pada satu `if` di controller yang bisa terlewat di jalur lain |
| `chk_listings_kuota` | `qty_reserved + qty_sold <= qty_total` | Batas terakhir kalau ada bug pada pengurangan stok bersamaan |
| `chk_listings_jam_ambil` | `pickup_end > pickup_start` | Jam ambil terbalik membuat listing tidak pernah muncul dan sulit dilacak |
| `chk_orders_total` | `total = subtotal + service_fee - discount` | Selisih rupiah pada struk adalah hal pertama yang dilihat mitra |

## Daur hidup status

**`listings.status`**
`draft` → `active` (butuh `ingredients_text`) → `paused` ⇄ `active`
→ `sold_out` (sisa habis) atau `expired` (lewat `pickup_end`, oleh penjadwal).

**`orders.status`**
`pending_pickup` → `completed` (kode ditukar) | `cancelled` (dibatalkan)
| `no_show` (lewat `pickup_end`, oleh penjadwal).

**`pickup_codes.status`**
`active` → `used` | `expired` | `cancelled`. Kode enam digit unik global,
bukan unik per toko, supaya kasir tidak pernah bisa menukar kode toko lain
karena kecocokan kebetulan.

## Siapa boleh menulis apa

| Tabel | `owner` | `cashier` | Konsumen |
|---|---|---|---|
| `listings`, `products`, `surprise_bag_templates` | ya | tidak | tidak |
| `waste_logs`, `waste_log_items` | ya | ya | tidak |
| `pickup_codes` (menukar) | ya | ya | tidak |
| `store_balances`, `balance_transactions` | baca | tidak boleh melihat | tidak |
| `store_members` | ya | tidak | tidak |
| `orders` | ubah status saja | ubah status saja | membuat |

Penolakan untuk kasir ditampilkan sopan di layar, bukan lewat crash.
Penegakannya dua lapis: middleware `ability:` dan Policy.

## Kolom yang wajib ditulis, sering terlupa

- `stores.pilot_consent_at` — tanggal mitra menyetujui ikut uji coba.
  Toko tanpa tanggal ini tidak boleh tampil di demo dan tidak boleh
  namanya dipakai di presentasi.
- `audit_logs` — wajib untuk penukaran kode, publikasi listing, edit catatan
  sisa, perubahan saldo, dan perubahan keanggotaan.
- `production_suggestions.rationale_text` — mitra berhak tahu dasar saran.
  Saran tanpa alasan akan diabaikan, dan pantas diabaikan.
  Cara hitungnya: rata-rata sisa `discarded` produk itu pada hari yang sama
  dalam 4 minggu terakhir (hari yang tidak dicatat tidak ikut dihitung,
  minimal 3 catatan), lalu produksi dipangkas `floor(rata-rata x 0,75)`.
  Seperempat sisanya dibiarkan karena masih bisa dijual lewat tas.

## Yang tidak boleh masuk database

Nomor HP lengkap di `audit_logs.meta`, kode OTP mentah di mana pun, dan
kode pickup mentah di log. `otp_codes` hanya menyimpan hash.
ID token Google juga tidak pernah disimpan; yang disimpan hanya `sub`-nya.
