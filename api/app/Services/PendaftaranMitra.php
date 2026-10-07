<?php

namespace App\Services;

use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use RuntimeException;

/**
 * Keputusan tim atas pendaftaran mitra (M03/M04). Dipakai perintah artisan mitra:setujui dan
 * mitra:tolak; nanti juga panel admin.
 */
class PendaftaranMitra
{
    /**
     * Membuat toko dari data pendaftaran: jam buka tiap hari, pemilik sebagai anggota, dan baris
     * saldo kosong. Mengembalikan id toko.
     */
    public function setujui(int $id): int
    {
        return DB::transaction(function () use ($id) {
            $p = DB::table('partner_applications')->where('id', $id)->lockForUpdate()->first();
            if ($p === null) {
                throw new RuntimeException("Pendaftaran #{$id} tidak ditemukan.");
            }
            if ($p->status !== 'pending') {
                throw new RuntimeException("Pendaftaran #{$id} berstatus {$p->status}, bukan pending.");
            }

            $sekarang = now();
            $tokoId = DB::table('stores')->insertGetId([
                'owner_user_id' => $p->user_id,
                'name' => $p->store_name,
                'slug' => $this->slugUnik($p->store_name),
                'category' => $p->category,
                'address' => $p->address,
                'latitude' => $p->latitude,
                'longitude' => $p->longitude,
                'halal_label' => $p->halal_certificate_no === null ? 'not_stated' : 'certified',
                'halal_certificate_no' => $p->halal_certificate_no,
                'nib' => $p->nib,
                // Nomor yang dipakai mendaftar sudah terverifikasi OTP; bisa diganti di M14.
                'whatsapp' => DB::table('users')->where('id', $p->user_id)->value('phone'),
                'pilot_consent_at' => $sekarang,
                'created_at' => $sekarang,
                'updated_at' => $sekarang,
            ]);

            for ($hari = 0; $hari <= 6; $hari++) {
                DB::table('store_hours')->insert([
                    'store_id' => $tokoId, 'day_of_week' => $hari, 'is_closed' => false,
                    'open_time' => $p->open_time, 'close_time' => $p->close_time,
                    'created_at' => $sekarang, 'updated_at' => $sekarang,
                ]);
            }
            DB::table('store_members')->insert([
                'store_id' => $tokoId, 'user_id' => $p->user_id, 'role' => 'owner',
                'invited_at' => $sekarang, 'created_at' => $sekarang, 'updated_at' => $sekarang,
            ]);
            DB::table('store_balances')->insert(['store_id' => $tokoId, 'created_at' => $sekarang, 'updated_at' => $sekarang]);

            DB::table('users')->where('id', $p->user_id)->whereNull('name')->update(['name' => $p->owner_name, 'updated_at' => $sekarang]);
            DB::table('partner_applications')->where('id', $id)->update([
                'status' => 'approved', 'store_id' => $tokoId, 'reviewed_at' => $sekarang, 'updated_at' => $sekarang,
            ]);
            DB::table('audit_logs')->insert([
                'user_id' => null, 'store_id' => $tokoId, 'action' => 'partner_application.approve',
                'subject_type' => 'partner_application', 'subject_id' => $id, 'meta' => json_encode([]),
                'created_at' => $sekarang, 'updated_at' => $sekarang,
            ]);

            return $tokoId;
        });
    }

    public function tolak(int $id, string $alasan): void
    {
        $diubah = DB::table('partner_applications')->where('id', $id)->where('status', 'pending')->update([
            'status' => 'rejected', 'rejection_reason' => Str::limit($alasan, 250), 'reviewed_at' => now(), 'updated_at' => now(),
        ]);
        if ($diubah === 0) {
            throw new RuntimeException("Pendaftaran #{$id} tidak ada atau bukan pending.");
        }
    }

    private function slugUnik(string $nama): string
    {
        $dasar = Str::slug($nama) ?: 'toko';
        $slug = $dasar;
        for ($i = 2; DB::table('stores')->where('slug', $slug)->exists(); $i++) {
            $slug = $dasar.'-'.$i;
        }

        return $slug;
    }
}
