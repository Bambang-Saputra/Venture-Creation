# ERD — Life of Foods

24 tabel proyek, ditambah tabel bawaan Laravel (`users`, `sessions`, `cache`,
`jobs`, `personal_access_tokens`, `migrations`).

Sumber kebenaran skema adalah berkas di `api/database/migrations/`.
Dokumen ini menjelaskan bentuk dan alasannya; kalau keduanya berbeda,
migrasi yang benar dan dokumen ini yang harus diperbaiki.

## Empat kelompok

| Kelompok | Tabel | Pertanyaan yang dijawab |
|---|---|---|
| Identitas | `users`, `consumer_profiles`, `otp_codes`, `user_allergens`, `allergens`, `store_members` | Siapa yang masuk, dan boleh berbuat apa |

Pengguna masuk lewat dua jalur: nomor HP + OTP (`users.phone`, `otp_codes`)
atau akun Google (`users.google_sub`, ADR-0006). Karena itu `users.phone`
nullable; satu akun bisa punya salah satu atau keduanya.
| Katalog | `stores`, `store_hours`, `products`, `surprise_bag_templates`, `listings`, `listing_items`, `listing_allergens` | Apa yang dijual hari ini |
| Transaksi | `orders`, `order_items`, `pickup_codes`, `store_balances`, `balance_transactions` | Siapa memesan apa, dan sudah diambil belum |
| Arah B | `waste_logs`, `waste_log_items`, `weekly_reports`, `production_suggestions` | Berapa yang terbuang, dan bagaimana menguranginya |
| Pendukung | `notifications`, `favorites`, `audit_logs` | Pemberitahuan, penanda, dan jejak |

## Diagram

```mermaid
erDiagram
    users ||--o| consumer_profiles : "punya"
    users ||--o{ user_allergens : "mencatat"
    allergens ||--o{ user_allergens : "dirujuk"
    users ||--o{ store_members : "menjadi anggota"
    stores ||--o{ store_members : "beranggota"
    users ||--o{ stores : "memiliki"

    stores ||--o{ store_hours : "buka pada"
    stores ||--o{ products : "menjual"
    stores ||--o{ surprise_bag_templates : "punya cetakan"
    stores ||--o{ listings : "memasang"
    surprise_bag_templates ||--o{ listings : "menjadi dasar"
    products ||--o{ listings : "menjadi menu satuan"
    listings ||--o{ listing_items : "berisi"
    listings ||--o{ listing_allergens : "mengandung"
    allergens ||--o{ listing_allergens : "dirujuk"

    users ||--o{ orders : "memesan"
    stores ||--o{ orders : "menerima"
    orders ||--|{ order_items : "berisi"
    listings ||--o{ order_items : "terjual sebagai"
    orders ||--|| pickup_codes : "menerbitkan"
    stores ||--|| store_balances : "punya saldo"
    stores ||--o{ balance_transactions : "mencatat"
    orders ||--o| balance_transactions : "menghasilkan"

    stores ||--o{ waste_logs : "mencatat sisa"
    waste_logs ||--o{ waste_log_items : "dirinci"
    products ||--o{ waste_log_items : "dirujuk"
    stores ||--o{ weekly_reports : "direkap"
    stores ||--o{ production_suggestions : "disarankan"
    products ||--o{ production_suggestions : "tentang"

    users ||--o{ notifications : "menerima"
    users ||--o{ favorites : "menandai"
    stores ||--o{ favorites : "ditandai"
    users ||--o{ audit_logs : "melakukan"
```

## Jalur yang paling sering dilewati

Alur beli, tujuh tabel:

`listings` → `orders` → `order_items` → `pickup_codes`
→ saat kode ditukar: `orders.status`, `listings.qty_sold`,
`balance_transactions`, `store_balances`, `audit_logs`.

Alur Arah B, empat tabel:

`waste_logs` + `waste_log_items` → agregasi terjadwal → `weekly_reports`
→ `production_suggestions`.
