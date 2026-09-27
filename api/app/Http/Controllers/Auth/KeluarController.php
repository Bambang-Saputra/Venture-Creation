<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Http\Response;

/**
 * POST /api/auth/logout (K20 Pengaturan -> Keluar).
 *
 * Hanya mencabut token perangkat ini. Sesi di perangkat lain tetap jalan.
 */
class KeluarController extends Controller
{
    public function __invoke(Request $request): Response
    {
        $request->user()->currentAccessToken()->delete();

        return response()->noContent();
    }
}
