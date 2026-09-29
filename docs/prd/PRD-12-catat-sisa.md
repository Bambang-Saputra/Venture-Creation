# PRD-12 · Catat sisa harian, dua mode

| | |
|---|---|
| Fitur | F-12 · MUST |
| Tujuan | Mengubah pemborosan yang tidak terlihat menjadi angka rupiah |
| Pengguna | Pemilik toko, kasir |
| Layar | M06 (hitung per item), M19 (timbang) |
| API | Kontrak §11: `GET` dan `POST /partner/stores/{store}/waste-logs` |
| Status | API selesai (`CatatSisaTest`) · Android: belum |

## Cerita pengguna

- Sebagai pemilik bakery, saya ingin tahu berapa rupiah yang saya buang hari ini, supaya punya alasan untuk mengubah jumlah produksi.
- Sebagai kasir yang menutup toko, saya ingin mencatat sisa dalam satu menit dengan tombol +/−, supaya tidak menambah beban kerja.

## Alur

1. **M06** menampilkan semua produk aktif, termasuk yang sisanya 0, masing-masing dengan tombol +/−. Nilai rupiah dihitung saat angka berubah.
2. Tambah item di luar daftar produk: nama, jumlah, dan nilai per satuan.
3. Pilih ke mana sisanya pergi: dibuang, disumbangkan, dimakan karyawan, atau terjual sebagai surplus.
4. "Simpan" → `POST`. Menyimpan lagi di hari yang sama mengganti isinya.
5. **M19** sama seperti M06, tetapi yang diisi berat, bukan jumlah.

## Kriteria penerimaan

| # | Kriteria | Status |
|---|---|---|
| 1 | Kasir boleh mencatat. Nilai dihitung dari HPP produk, atau harga jual kalau HPP kosong | Diuji · `kasir_mencatat_per_item_nilai_memakai_hpp` |
| 2 | Menyimpan ulang mengganti isi hari itu, dan hanya yang dibuang (`discarded`) yang dihitung sebagai pemborosan | Diuji · `simpan_ulang_mengganti_isi_hari_itu_dan_hanya_discarded_yang_dihitung` |
| 3 | Mode timbang menghitung nilai dari berat per satuan produk | Diuji · `m19_timbang_nilai_dari_berat_per_satuan` |
| 4 | Ada perbandingan dengan hari yang sama pekan lalu | Diuji · `perbandingan_dengan_hari_yang_sama_pekan_lalu` |
| 5 | Tanggal yang belum terjadi ditolak; catatan kemarin masih bisa diubah; lewat akhir hari berikutnya terkunci (409) | Diuji · `tanggal_mendatang_ditolak_kemarin_masih_boleh` |
| 6 | Produk milik toko lain ditolak | Diuji · `produk_toko_lain_ditolak` |
| 7 | Nilai rupiah disimpan saat dicatat, dan tidak dihitung ulang kalau HPP berubah | Ada di kode (`waste_log_items.value_rupiah`) |
| 8 | Setiap perubahan catatan tercatat di `audit_logs` | Ada di kode · `CatatSisaController` |
| 9 | Nilai rupiah sudah tampil saat mengetik, sebelum disimpan | Belum |
| 10 | Mencatat 5 produk selesai dalam waktu kurang dari 1 menit | Belum diukur. Ukur saat uji prototipe dengan mitra |

## Kosong dan galat

- Toko tanpa produk: M06 langsung membuka input item bebas.
- 409 terkunci: "Catatan tanggal ini sudah dikunci", beserta tampilan baca saja.
- Hari ini belum dicatat: pengingat di dashboard (PRD-24).

## Di luar lingkup

- Timbangan Bluetooth, foto sisa, dan hitungan emisi karbon.

## Catatan

Menurut temuan S4 di riset minggu 2, hambatan terbesar mitra adalah tidak tahu berapa banyak yang mereka buang. Fitur ini baru berhasil kalau benar-benar diisi setiap hari. Karena itu kriteria 10 sama pentingnya dengan kriteria yang sudah diuji.
