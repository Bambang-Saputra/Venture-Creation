# Ringkasan

<!-- Dua sampai tiga kalimat: apa yang berubah dan kenapa. Bukan daftar berkas. -->

## Perubahan

-
-

## Cara menguji

<!-- Langkah yang benar-benar bisa diikuti orang lain, termasuk data uji yang dipakai. -->

1.
2.

## Tangkapan layar atau rekaman

<!-- Wajib untuk setiap perubahan UI. Sebelum dan sesudah bila memungkinkan. -->

## Checklist

- [ ] CI hijau
- [ ] Migrasi punya `down()` dan sudah diuji `migrate` → `rollback` → `migrate`
- [ ] Tidak ada rahasia, `.env`, atau data mitra sungguhan yang ikut ter-commit
- [ ] `api/.env.example` diperbarui bila ada konfigurasi baru
- [ ] PRD atau ADR diperbarui bila perilaku produk berubah
- [ ] Diff di bawah sekitar 400 baris, atau sudah dijelaskan kenapa tidak bisa dipecah

Terkait: #
