<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;

/** Daftar pendaftaran mitra yang menunggu verifikasi. Lihat mitra:setujui. */
class DaftarPendaftaranMitra extends Command
{
    protected $signature = 'mitra:pendaftaran {--semua : termasuk yang sudah disetujui atau ditolak}';

    protected $description = 'Tampilkan pendaftaran mitra dari app (M03)';

    public function handle(): int
    {
        $baris = DB::table('partner_applications as p')
            ->join('users as u', 'u.id', '=', 'p.user_id')
            ->when(! $this->option('semua'), fn ($q) => $q->where('p.status', 'pending'))
            ->orderBy('p.id')
            ->get(['p.id', 'p.status', 'p.store_name', 'p.category', 'p.owner_name', 'u.phone', 'p.nib', 'p.address', 'p.created_at']);

        if ($baris->isEmpty()) {
            $this->info('Tidak ada pendaftaran yang menunggu.');

            return self::SUCCESS;
        }

        $this->table(
            ['id', 'status', 'toko', 'kategori', 'pemilik', 'HP', 'NIB', 'alamat', 'dikirim'],
            $baris->map(fn ($b) => [$b->id, $b->status, $b->store_name, $b->category, $b->owner_name, $b->phone, $b->nib ?? '-', $b->address, $b->created_at])->all(),
        );

        return self::SUCCESS;
    }
}
