<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use RuntimeException;

/**
 * Membuat akun mitra uji coba dari berkas CSV lokal.
 *
 * Berkasnya ada di data-pilot/mitra.csv dan tidak pernah ikut ter-commit,
 * karena isinya nama usaha, alamat, dan nomor HP orang sungguhan.
 * Contoh isian ada di data-pilot/contoh-mitra.csv.txt yang boleh di-commit.
 *
 * Kolom yang dibaca, urutannya bebas asal judulnya cocok:
 *   nama_toko, kategori, alamat, latitude, longitude,
 *   nama_pemilik, whatsapp_pemilik, label_halal, no_sertifikat_halal,
 *   kandungan_default, tanggal_persetujuan, email_pemilik (opsional)
 *
 * tanggal_persetujuan wajib terisi. Baris tanpa tanggal itu dilewati,
 * karena tanggal persetujuan adalah satu-satunya bukti mitra memang
 * setuju ikut uji coba dan bersedia namanya dipakai saat presentasi.
 */
class SeederPilot extends Seeder
{
    private const BERKAS = 'data-pilot/mitra.csv';

    private const KOLOM_WAJIB = [
        'nama_toko', 'kategori', 'alamat', 'nama_pemilik',
        'whatsapp_pemilik', 'tanggal_persetujuan',
    ];

    private const KATEGORI_SAH = ['cafe', 'bakery', 'resto', 'catering', 'grocery'];

    private const HALAL_SAH = ['certified', 'self_claim', 'not_stated'];

    public function run(): void
    {
        $jalur = base_path(self::BERKAS);

        if (! is_readable($jalur)) {
            throw new RuntimeException(
                'Berkas '.self::BERKAS.' tidak ditemukan. Salin data-pilot/contoh-mitra.csv.txt '.
                'menjadi '.self::BERKAS.' lalu isi datanya. Berkas itu sengaja di-gitignore.'
            );
        }

        $dibuat = 0;
        $dilewati = 0;

        foreach ($this->bacaCsv($jalur) as $nomor => $data) {
            if (($data['tanggal_persetujuan'] ?? '') === '') {
                $this->command?->warn("Baris {$nomor} dilewati: tanggal_persetujuan kosong.");
                $dilewati++;

                continue;
            }

            $this->simpanMitra($data) ? $dibuat++ : $dilewati++;
        }

        $this->command?->info("SeederPilot selesai: {$dibuat} mitra dibuat, {$dilewati} dilewati.");
    }

    /**
     * @return array<int,array<string,string>>
     */
    private function bacaCsv(string $jalur): array
    {
        $tangan = fopen($jalur, 'r');
        // escape kosong mematikan pemrosesan escape CSV, sekaligus menghindari
        // peringatan deprecated di PHP 8.4 soal nilai bawaan parameter ini.
        $judul = fgetcsv($tangan, escape: '');

        if ($judul === false) {
            fclose($tangan);
            throw new RuntimeException('Berkas '.self::BERKAS.' kosong.');
        }

        $judul = array_map(fn ($j) => Str::lower(trim((string) $j)), $judul);
        $kurang = array_diff(self::KOLOM_WAJIB, $judul);

        if ($kurang !== []) {
            fclose($tangan);
            throw new RuntimeException('Kolom wajib belum ada di CSV: '.implode(', ', $kurang));
        }

        $hasil = [];
        $nomor = 1;

        while (($isi = fgetcsv($tangan, escape: '')) !== false) {
            $nomor++;

            if ($isi === [null] || $isi === []) {
                continue;
            }

            $isi = array_pad(array_slice($isi, 0, count($judul)), count($judul), '');
            $hasil[$nomor] = array_combine($judul, array_map(fn ($n) => trim((string) $n), $isi));
        }

        fclose($tangan);

        return $hasil;
    }

    /**
     * Idempoten: dijalankan dua kali tidak membuat toko kembar.
     * Kuncinya nomor WhatsApp pemilik, karena nama toko bisa saja diketik
     * berbeda di baris berikutnya.
     *
     * @param  array<string,string>  $data
     */
    private function simpanMitra(array $data): bool
    {
        $hp = $this->normalkanNomor($data['whatsapp_pemilik']);

        if ($hp === null) {
            $this->command?->warn('Dilewati: nomor WhatsApp tidak dikenali untuk '.$data['nama_toko'].'.');

            return false;
        }

        $kategori = Str::lower($data['kategori'] ?? '');
        if (! in_array($kategori, self::KATEGORI_SAH, true)) {
            $this->command?->warn('Dilewati: kategori "'.$kategori.'" tidak dikenal untuk '.$data['nama_toko'].'.');

            return false;
        }

        $halal = Str::lower($data['label_halal'] ?? '');
        if (! in_array($halal, self::HALAL_SAH, true)) {
            // Ragu berarti tidak disebutkan. Aplikasi tidak pernah menebak status halal.
            $halal = 'not_stated';
        }

        $sekarang = now();

        return DB::transaction(function () use ($data, $hp, $kategori, $halal, $sekarang) {
            $userId = DB::table('users')->where('phone', $hp)->value('id');
            // Opsional. Tanpa email, mitra hanya bisa masuk lewat OTP (ADR-0006).
            $email = filter_var($data['email_pemilik'] ?? '', FILTER_VALIDATE_EMAIL) ? Str::lower($data['email_pemilik']) : null;

            if ($userId !== null && $email !== null) {
                DB::table('users')->where('id', $userId)->whereNull('email')->update(['email' => $email]);
            }

            if ($userId === null) {
                $userId = DB::table('users')->insertGetId([
                    'phone' => $hp,
                    'email' => $email,
                    'name' => $data['nama_pemilik'],
                    'phone_verified_at' => $sekarang,
                    'role' => 'partner',
                    'created_at' => $sekarang,
                    'updated_at' => $sekarang,
                ]);
            }

            if (DB::table('stores')->where('owner_user_id', $userId)->exists()) {
                $this->command?->line('Sudah ada, dilewati: '.$data['nama_toko']);

                return false;
            }

            $storeId = DB::table('stores')->insertGetId([
                'owner_user_id' => $userId,
                'name' => $data['nama_toko'],
                'slug' => Str::slug($data['nama_toko']).'-'.Str::lower(Str::random(4)),
                'category' => $kategori,
                'address' => $data['alamat'],
                'latitude' => is_numeric($data['latitude'] ?? '') ? (float) $data['latitude'] : null,
                'longitude' => is_numeric($data['longitude'] ?? '') ? (float) $data['longitude'] : null,
                'whatsapp' => $hp,
                'halal_label' => $halal,
                'halal_certificate_no' => $halal === 'certified' ? ($data['no_sertifikat_halal'] ?: null) : null,
                'default_ingredients_text' => $data['kandungan_default'] ?: null,
                'pilot_consent_at' => $data['tanggal_persetujuan'],
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            DB::table('store_members')->insert([
                'store_id' => $storeId,
                'user_id' => $userId,
                'role' => 'owner',
                'invited_at' => $sekarang,
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            DB::table('store_balances')->insert([
                'store_id' => $storeId,
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            for ($hari = 0; $hari <= 6; $hari++) {
                DB::table('store_hours')->insert([
                    'store_id' => $storeId,
                    'day_of_week' => $hari,
                    'open_time' => '08:00:00',
                    'close_time' => '21:00:00',
                    'created_at' => $sekarang,
                    'updated_at' => $sekarang,
                ]);
            }

            return true;
        });
    }

    /**
     * Menyeragamkan 08xx, +628xx, dan 628xx menjadi 628xx.
     * Nomor yang tidak berbentuk nomor Indonesia dikembalikan sebagai null,
     * karena nomor salah berarti mitra tidak akan pernah bisa masuk.
     */
    private function normalkanNomor(string $mentah): ?string
    {
        $angka = preg_replace('/[^0-9]/', '', $mentah) ?? '';

        if (str_starts_with($angka, '0')) {
            $angka = '62'.substr($angka, 1);
        } elseif (str_starts_with($angka, '8')) {
            $angka = '62'.$angka;
        }

        return preg_match('/^628[1-9][0-9]{6,11}$/', $angka) === 1 ? $angka : null;
    }
}
