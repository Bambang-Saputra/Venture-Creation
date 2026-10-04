<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Partner\AksesToko;
use App\Services\RatingToko;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * Rating dan ulasan. Pembeli menilai pesanan yang sudah diambil (K15);
 * mitra membaca ulasan tokonya (M14).
 */
class UlasanController extends Controller
{
    use AksesToko;

    /**
     * POST /api/orders/{order}/review. Mengirim lagi mengganti ulasan yang
     * lama, selama belum lewat RatingToko::BATAS_HARI sejak pesanan diambil.
     */
    public function simpan(Request $request, int $order): JsonResponse
    {
        $user = $request->user();
        abort_if($user->role !== 'consumer', 403, 'Ulasan hanya bisa diberikan dari akun konsumen.');

        $f = $request->validate([
            'rating' => ['required', 'integer', 'between:1,5'],
            'comment' => ['nullable', 'string', 'max:500'],
        ]);
        $komentar = isset($f['comment']) ? trim($f['comment']) : null;

        $baru = DB::transaction(function () use ($user, $order, $f, $komentar) {
            $o = DB::table('orders')->where('id', $order)->where('user_id', $user->id)->lockForUpdate()->first();
            abort_if($o === null, 404, 'Pesanan tidak ditemukan.');
            abort_if($o->status !== 'completed', 409, 'Ulasan bisa diberikan setelah pesanan diambil.');
            abort_unless(RatingToko::masihBisaDiulas($o->status, $o->completed_at), 409,
                'Ulasan hanya bisa diberikan sampai '.RatingToko::BATAS_HARI.' hari setelah pesanan diambil.');

            $ada = DB::table('reviews')->where('order_id', $o->id)->exists();
            $isi = ['rating' => $f['rating'], 'comment' => $komentar === '' ? null : $komentar, 'updated_at' => now()];
            $ada
                ? DB::table('reviews')->where('order_id', $o->id)->update($isi)
                : DB::table('reviews')->insert([...$isi, 'order_id' => $o->id, 'store_id' => $o->store_id,
                    'user_id' => $user->id, 'created_at' => now()]);

            return ! $ada;
        });

        $u = DB::table('reviews')->where('order_id', $order)->first();

        return response()->json(['data' => [
            'rating' => (int) $u->rating,
            'comment' => $u->comment,
            'created_at' => Carbon::parse($u->created_at)->toIso8601String(),
            'updated_at' => Carbon::parse($u->updated_at)->toIso8601String(),
        ]], $baru ? 201 : 200);
    }

    /** GET /api/partner/stores/{store}/reviews. Pemilik dan kasir. */
    public function daftarToko(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store, pemilikSaja: false);

        $halaman = DB::table('reviews')
            ->join('users', 'users.id', '=', 'reviews.user_id')
            ->where('reviews.store_id', $store)
            ->orderByDesc('reviews.created_at')->orderByDesc('reviews.id')
            ->select('reviews.id', 'reviews.rating', 'reviews.comment', 'reviews.created_at', 'users.name as buyer_name')
            ->paginate(20);

        $halaman->through(fn (object $r) => [
            'id' => $r->id,
            'rating' => (int) $r->rating,
            'comment' => $r->comment,
            // Nama depan saja, sama dengan pesanan masuk. Akun yang dihapus jadi "Pembeli".
            'buyer_name' => $r->buyer_name === null ? 'Pembeli' : strtok($r->buyer_name, ' '),
            'created_at' => Carbon::parse($r->created_at)->toIso8601String(),
        ]);

        return response()->json([...$halaman->toArray(), 'summary' => RatingToko::untukToko($store)]);
    }
}
