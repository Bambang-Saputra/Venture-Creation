<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Token yang terbit sebelum akun dinonaktifkan tetap sah di Sanctum.
 * Middleware ini yang memutusnya di setiap rute ber-auth.
 */
class PastikanAkunAktif
{
    public function handle(Request $request, Closure $next): Response
    {
        if ($request->user() !== null && ! $request->user()->is_active) {
            return response()->json(['message' => 'Akun ini dinonaktifkan. Hubungi tim Life of Foods.'], 403);
        }

        return $next($request);
    }
}
