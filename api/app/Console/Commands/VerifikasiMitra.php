<?php

namespace App\Console\Commands;

use App\Services\PendaftaranMitra;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;
use RuntimeException;

/**
 * Verifikasi pendaftaran mitra oleh tim selama pilot (belum ada panel admin).
 *
 *   php artisan mitra:pendaftaran              daftar yang menunggu
 *   php artisan mitra:setujui 3                buat tokonya, mitra langsung bisa masuk M05
 *   php artisan mitra:setujui 3 --tolak="..."  tolak dengan alasan yang tampil di M04
 */
class VerifikasiMitra extends Command
{
    protected $signature = 'mitra:setujui {id : id pendaftaran} {--tolak= : tolak dengan alasan ini}';

    protected $description = 'Setujui atau tolak pendaftaran mitra dari app (M03)';

    public function handle(PendaftaranMitra $layanan): int
    {
        $id = (int) $this->argument('id');
        $alasan = $this->option('tolak');

        try {
            if ($alasan !== null) {
                $layanan->tolak($id, $alasan);
                $this->info("Pendaftaran #{$id} ditolak.");
            } else {
                $toko = $layanan->setujui($id);
                $nama = DB::table('stores')->where('id', $toko)->value('name');
                $this->info("Pendaftaran #{$id} disetujui: toko #{$toko} {$nama}.");
            }
        } catch (RuntimeException $e) {
            $this->error($e->getMessage());

            return self::FAILURE;
        }

        return self::SUCCESS;
    }
}
