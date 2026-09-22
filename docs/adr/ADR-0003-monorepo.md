# ADR-0003 · Satu repositori untuk API, Android, dan dokumen

**Status:** diterima · 22 September 2026

## Konteks
Perubahan sekecil menambah kolom `halal_label` menyentuh migrasi, endpoint, model POJO Android, dan dokumen PRD sekaligus.

## Keputusan
Monorepo `life-of-foods` berisi `api/`, `android/`, `db/`, dan `docs/`.

## Alasan
Multi-repo memaksa tiga pull request terkoordinasi untuk satu perubahan, dan itu biaya yang tidak sanggup ditanggung tim mahasiswa dengan lima minggu tersisa. Tim juga sudah terbiasa monorepo di proyek sebelumnya.

## Konsekuensi
- Workflow CI dipicu berdasarkan path, supaya perubahan dokumen tidak membangun APK.
- Repo diletakkan di `D:\Proyek\life-of-foods`, bukan di dalam folder kuliah yang path-nya mengandung spasi, karena spasi di jalur proyek adalah sumber kegagalan build Gradle yang sulit dilacak.

## Alternatif yang ditolak
Repo terpisah untuk API dan Android.
