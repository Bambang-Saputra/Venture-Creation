<?php

namespace App\Services;

use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * Voucher pembeli (ADR-0008): klaim dari pop up, hadiah Gosok & Menang, dan potongan di pesanan.
 * Potongan ditanggung Life of Foods; lihat subsidiKeMitra().
 */
class Voucher
{
    /** Voucher milik pembeli yang masih bisa dipakai, yang paling cepat kedaluwarsa dulu. */
    public function milik(int $userId): array
    {
        return DB::table('user_vouchers as uv')
            ->join('vouchers as v', 'v.id', '=', 'uv.voucher_id')
            ->where('uv.user_id', $userId)
            ->whereNull('uv.used_at')
            ->where('uv.expires_at', '>', now())
            ->orderBy('uv.expires_at')
            ->get(['uv.id', 'uv.expires_at', 'v.code', 'v.title', 'v.type', 'v.value', 'v.max_discount_rupiah', 'v.min_spend_rupiah'])
            ->map(fn ($b) => $this->bentuk($b))
            ->all();
    }

    /** Voucher pop up yang masih bisa diklaim pembeli ini, atau null. */
    public function promo(int $userId): ?array
    {
        $v = DB::table('vouchers')
            ->where('source', 'popup')->where('is_active', true)
            ->where(fn ($q) => $q->whereNull('claimable_until')->orWhere('claimable_until', '>', now()))
            ->where(fn ($q) => $q->whereNull('quota')->orWhereColumn('claimed_count', '<', 'quota'))
            ->whereNotExists(fn ($q) => $q->from('user_vouchers')->whereColumn('voucher_id', 'vouchers.id')->where('user_id', $userId))
            ->orderByDesc('id')
            ->first();

        return $v === null ? null : [
            'id' => $v->id, 'code' => $v->code, 'title' => $v->title, 'type' => $v->type, 'value' => $v->value,
            'max_discount_rupiah' => $v->max_discount_rupiah, 'min_spend_rupiah' => $v->min_spend_rupiah,
            'valid_days' => $v->valid_days,
        ];
    }

    /** Klaim voucher pop up. Sekali per pembeli per voucher. */
    public function klaim(int $userId, int $voucherId): array
    {
        return DB::transaction(function () use ($userId, $voucherId) {
            $v = DB::table('vouchers')->where('id', $voucherId)->where('source', 'popup')->lockForUpdate()->first();
            abort_if($v === null || ! $v->is_active, 404, 'Voucher ini sudah tidak tersedia.');
            abort_if($v->claimable_until !== null && now()->gt($v->claimable_until), 409, 'Waktu klaim voucher ini sudah habis.');
            abort_if($v->quota !== null && $v->claimed_count >= $v->quota, 409, 'Kuota voucher ini sudah habis. Coba lagi besok.');
            abort_if(DB::table('user_vouchers')->where('user_id', $userId)->where('voucher_id', $v->id)->exists(),
                409, 'Kamu sudah mengklaim voucher ini.');

            return $this->berikan($userId, $v);
        });
    }

    /**
     * Gosok & Menang, sekali sehari. Hadiah diundi dari voucher source "game" menurut weight; ada
     * peluang "belum beruntung" sebesar $bobotZonk.
     */
    public function gosok(int $userId, int $bobotZonk = 30): array
    {
        return DB::transaction(function () use ($userId, $bobotZonk) {
            $hariIni = now()->toDateString();
            abort_if(DB::table('game_plays')->where('user_id', $userId)->where('play_date', $hariIni)->exists(),
                409, 'Kamu sudah main hari ini. Datang lagi besok untuk kesempatan baru!');

            $hadiah = DB::table('vouchers')->where('source', 'game')->where('is_active', true)->where('weight', '>', 0)->get();
            $undian = random_int(1, max(1, $hadiah->sum('weight') + $bobotZonk));
            $menang = null;
            foreach ($hadiah as $v) {
                $undian -= $v->weight;
                if ($undian <= 0) {
                    $menang = $v;
                    break;
                }
            }

            $milik = $menang === null ? null : $this->berikan($userId, $menang);
            DB::table('game_plays')->insert([
                'user_id' => $userId, 'play_date' => $hariIni, 'voucher_id' => $menang?->id,
                'user_voucher_id' => $milik['id'] ?? null, 'created_at' => now(), 'updated_at' => now(),
            ]);

            return ['won' => $milik !== null, 'voucher' => $milik];
        });
    }

    public function sudahMainHariIni(int $userId): bool
    {
        return DB::table('game_plays')->where('user_id', $userId)->where('play_date', now()->toDateString())->exists();
    }

    /**
     * Potongan untuk subtotal tertentu. Galat kalau voucher bukan milik pembeli, sudah dipakai,
     * kedaluwarsa, atau belanja belum mencapai minimal.
     *
     * @return array{id: int, title: string, discount_rupiah: int}
     */
    public function hitungPotongan(int $userId, int $userVoucherId, int $subtotal, bool $kunci = false): array
    {
        $b = DB::table('user_vouchers as uv')
            ->join('vouchers as v', 'v.id', '=', 'uv.voucher_id')
            ->where('uv.id', $userVoucherId)->where('uv.user_id', $userId)
            ->when($kunci, fn ($q) => $q->lockForUpdate())
            ->first(['uv.id', 'uv.used_at', 'uv.expires_at', 'v.title', 'v.type', 'v.value', 'v.max_discount_rupiah', 'v.min_spend_rupiah']);

        if ($b === null || $b->used_at !== null || now()->gte($b->expires_at)) {
            throw ValidationException::withMessages(['user_voucher_id' => 'Voucher ini sudah tidak bisa dipakai.']);
        }
        if ($subtotal < $b->min_spend_rupiah) {
            $min = 'Rp'.number_format($b->min_spend_rupiah, 0, ',', '.');
            throw ValidationException::withMessages(['user_voucher_id' => "Voucher ini berlaku untuk belanja minimal {$min}."]);
        }

        $potong = $b->type === 'percent' ? intdiv($subtotal * $b->value, 100) : $b->value;
        if ($b->max_discount_rupiah !== null) {
            $potong = min($potong, $b->max_discount_rupiah);
        }

        return ['id' => $b->id, 'title' => $b->title, 'discount_rupiah' => min($potong, $subtotal)];
    }

    public function tandaiDipakai(int $userVoucherId, int $orderId): void
    {
        DB::table('user_vouchers')->where('id', $userVoucherId)
            ->update(['used_at' => now(), 'order_id' => $orderId, 'updated_at' => now()]);
    }

    /** Pesanan dibatalkan pembeli: voucher kembali bisa dipakai selama belum kedaluwarsa. */
    public function kembalikan(int $orderId): void
    {
        DB::table('user_vouchers')->where('order_id', $orderId)
            ->update(['used_at' => null, 'order_id' => null, 'updated_at' => now()]);
    }

    /**
     * Kode pickup ditukar: potongan voucher ditambahkan ke saldo mitra sebagai penyesuaian, karena
     * mitra hanya menerima total setelah potongan dari pembeli di kasir.
     */
    public function subsidiKeMitra(object $order, int $kasirId): void
    {
        if ($order->discount_rupiah <= 0) {
            return;
        }
        $saldo = DB::table('store_balances')->where('store_id', $order->store_id)->lockForUpdate()->first();
        $tersedia = $saldo->available_rupiah + $order->discount_rupiah;
        DB::table('store_balances')->where('store_id', $order->store_id)->update([
            'available_rupiah' => $tersedia,
            'lifetime_rupiah' => $saldo->lifetime_rupiah + $order->discount_rupiah,
            'updated_at' => now(),
        ]);
        DB::table('balance_transactions')->insert([
            'store_id' => $order->store_id, 'order_id' => $order->id, 'type' => 'adjustment',
            'amount_rupiah' => $order->discount_rupiah, 'balance_after_rupiah' => $tersedia,
            'description' => "Voucher pembeli ditanggung Life of Foods, pesanan {$order->code}",
            'created_by_user_id' => $kasirId, 'created_at' => now(), 'updated_at' => now(),
        ]);
    }

    private function berikan(int $userId, object $v): array
    {
        $id = DB::table('user_vouchers')->insertGetId([
            'user_id' => $userId, 'voucher_id' => $v->id, 'claimed_at' => now(),
            'expires_at' => now()->addDays($v->valid_days)->endOfDay(), 'created_at' => now(), 'updated_at' => now(),
        ]);
        DB::table('vouchers')->where('id', $v->id)->increment('claimed_count');

        return $this->bentuk(DB::table('user_vouchers as uv')->join('vouchers as v', 'v.id', '=', 'uv.voucher_id')
            ->where('uv.id', $id)
            ->first(['uv.id', 'uv.expires_at', 'v.code', 'v.title', 'v.type', 'v.value', 'v.max_discount_rupiah', 'v.min_spend_rupiah']));
    }

    private function bentuk(object $b): array
    {
        return [
            'id' => $b->id, 'code' => $b->code, 'title' => $b->title, 'type' => $b->type, 'value' => (int) $b->value,
            'max_discount_rupiah' => $b->max_discount_rupiah === null ? null : (int) $b->max_discount_rupiah,
            'min_spend_rupiah' => (int) $b->min_spend_rupiah,
            'expires_at' => Carbon::parse($b->expires_at)->toIso8601String(),
        ];
    }
}
