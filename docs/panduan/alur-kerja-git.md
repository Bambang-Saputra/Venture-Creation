# Alur kerja git

## Sekali di awal

```powershell
winget install --id GitHub.cli
gh auth login --hostname github.com --git-protocol https --web
```

Tanpa GitHub CLI, skill `/qc` berhenti di langkah pertama.

## Branch

Format: `<tipe>/<lingkup>-<deskripsi-singkat>`, huruf kecil, dipisah tanda hubung.

Tipe: `fitur` · `perbaikan` · `dokumen` · `refaktor` · `uji` · `ci`

```
fitur/api-kode-pickup
perbaikan/android-crash-daftar-kosong
dokumen/prd-catat-sisa
```

Hanya ada `main`, tanpa `dev`. Empat orang selama lima minggu tidak butuh branch integrasi; itu cuma menambah satu langkah merge yang bisa gagal.

## Alur harian

1. `git switch main && git pull`
2. `git switch -c fitur/api-kode-pickup`
3. Kerjakan **satu** hal sampai selesai
4. **`/qc`** — membaca kode yang di-stage, mencari bug, memeriksa konflik dependensi, menyusun pesan commit, commit, lalu push
5. `gh pr create` — isi template, lampirkan tangkapan layar untuk perubahan UI
6. Tunggu CI hijau, minta satu persetujuan, **squash merge**

`/qc main` hanya untuk perbaikan dokumen kecil yang tidak butuh review orang lain.

## Pesan commit

```
<tipe>(<lingkup>): <subjek imperatif huruf kecil, maksimal 72 karakter>

<badan opsional: jelaskan alasan, bukan mengulang diff>

Terkait: #12
```

Tipe: `feat` `fix` `docs` `refactor` `test` `chore` `build` `ci` `perf`
Lingkup: `api` `android` `db` `docs` `ci` `infra`

```
feat(api): tambah endpoint penukaran kode pickup
fix(android): cegah crash saat daftar listing kosong
docs(db): tambah kamus data tabel waste_logs
```

**Dilarang:** baris atribusi AI apa pun, termasuk `Co-Authored-By: Claude`, `Generated with`, dan emoji robot. Aturan ini berlaku untuk seluruh anggota tim dan alat bantu apa pun, dan ditegakkan otomatis oleh workflow `pr-judul.yml`.

## Yang ditolak `/qc`

- `--no-verify`, melewati hook
- `--no-gpg-sign`, melewati tanda tangan
- `--amend`, menulis ulang riwayat. Kalau ada bug setelah commit, buat commit baru
- force push. Kalau push ditolak karena non-fast-forward, berhenti dan laporkan
- push tanpa review kode lebih dulu

## Pull request

Judul mengikuti format pesan commit. Badan memakai template repo: Ringkasan, Perubahan, Cara menguji, Tangkapan layar, Checklist.

Batas sekitar 400 baris diff. Lebih dari itu, pecah. Satu persetujuan, dan penulis tidak menyetujui PR-nya sendiri.

## Pengaturan `main` di GitHub

Settings → Branches → Add rule untuk `main`:
- Require a pull request before merging, dengan 1 approval
- Require status checks to pass: `api-ci`, `android-ci`, `pr-judul`
- Require linear history
- Allow squash merging saja, matikan merge commit dan rebase

## Yang tidak pernah masuk repo

`.env` · kunci Maps · keystore · dump database berisi data mitra sungguhan · CSV data pilot · tangkapan layar yang memuat nomor HP pelanggan.

Kalau rahasia terlanjur ter-commit: **cabut dan putar kuncinya dulu**, baru bersihkan riwayat. Menghapus commit tanpa memutar kunci hanya memberi rasa aman palsu.
