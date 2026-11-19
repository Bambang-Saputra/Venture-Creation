<?php

namespace App\Http\Controllers;

use App\Services\Voucher;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Voucher pembeli (K22, pop up Beranda, Gosok & Menang). ADR-0008.
 */
class VoucherController extends Controller
{
    public function __construct(private readonly Voucher $voucher) {}

    /** GET /api/vouchers: voucher milikku, voucher pop up yang bisa diklaim, dan status gosok hari ini. */
    public function daftar(Request $request): JsonResponse
    {
        $this->khususKonsumen($request);
        $id = $request->user()->id;

        return response()->json(['data' => [
            'mine' => $this->voucher->milik($id),
            'promo' => $this->voucher->promo($id),
            'can_scratch_today' => ! $this->voucher->sudahMainHariIni($id),
        ]]);
    }

    /** POST /api/vouchers/{voucher}/claim */
    public function klaim(Request $request, int $voucher): JsonResponse
    {
        $this->khususKonsumen($request);

        return response()->json(['data' => $this->voucher->klaim($request->user()->id, $voucher)], 201);
    }

    /** POST /api/games/scratch: Gosok & Menang, sekali sehari. */
    public function gosok(Request $request): JsonResponse
    {
        $this->khususKonsumen($request);

        return response()->json(['data' => $this->voucher->gosok($request->user()->id)]);
    }

    private function khususKonsumen(Request $request): void
    {
        abort_if($request->user()->role !== 'consumer', 403, 'Voucher hanya untuk akun pembeli.');
    }
}
