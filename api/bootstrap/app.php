<?php

use App\Http\Middleware\PastikanAkunAktif;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Exceptions\ThrottleRequestsException;
use Illuminate\Http\Request;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->alias(['aktif' => PastikanAkunAktif::class]);
        // ngrok berjalan di laptop yang sama dan meneruskan X-Forwarded-Proto/Host. Hanya
        // loopback yang dipercaya, supaya klien lain tidak bisa memalsukan header itu.
        // Tanpa ini asset() membuat alamat foto http://127.0.0.1 yang tidak bisa dibuka HP.
        $middleware->trustProxies(at: ['127.0.0.1', '::1']);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        // Pesan bawaan Laravel "Too Many Attempts." tampil apa adanya di Android.
        $exceptions->render(function (ThrottleRequestsException $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }
            $tunggu = max(1, (int) ($e->getHeaders()['Retry-After'] ?? 60));

            return response()->json([
                'message' => "Terlalu sering mencoba. Tunggu {$tunggu} detik lalu coba lagi.",
                'code' => 'too_many_requests',
                'retry_after' => $tunggu,
            ], 429, $e->getHeaders());
        });
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api/*') || $request->expectsJson(),
        );
    })->create();
