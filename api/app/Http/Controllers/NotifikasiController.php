<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * K17 Notifikasi (F-18). Dipakai konsumen dan mitra; isinya selalu milik
 * akun yang sedang masuk. Dibuat oleh App\Services\Notifikasi.
 */
class NotifikasiController extends Controller
{
    /** GET /api/notifications?unread=1 */
    public function daftar(Request $request): JsonResponse
    {
        $f = $request->validate(['unread' => ['sometimes', 'boolean']]);
        $user = $request->user();

        $halaman = DB::table('notifications')->where('user_id', $user->id)
            ->when($f['unread'] ?? false, fn ($q) => $q->whereNull('read_at'))
            ->orderByDesc('created_at')->orderByDesc('id')
            ->paginate(20);

        $halaman->through(fn (object $n) => [
            'id' => $n->id,
            'type' => $n->type,
            'title' => $n->title,
            'body' => $n->body,
            // Tujuan ketukan, contoh {"screen":"K14","order_id":12}.
            'data' => $n->data === null ? null : json_decode($n->data, true),
            'is_read' => $n->read_at !== null,
            'created_at' => Carbon::parse($n->created_at)->toIso8601String(),
        ]);

        $json = $halaman->toArray();
        // Angka lencana di ikon lonceng.
        $json['unread_count'] = DB::table('notifications')->where('user_id', $user->id)->whereNull('read_at')->count();

        return response()->json($json);
    }

    /** POST /api/notifications/{id}/read. Notifikasi orang lain dijawab 404. */
    public function baca(Request $request, int $notification): Response
    {
        $milik = DB::table('notifications')->where('id', $notification)->where('user_id', $request->user()->id);
        abort_unless($milik->exists(), 404, 'Notifikasi tidak ditemukan.');
        $milik->whereNull('read_at')->update(['read_at' => now(), 'updated_at' => now()]);

        return response()->noContent();
    }

    /** POST /api/notifications/read-all */
    public function bacaSemua(Request $request): Response
    {
        DB::table('notifications')->where('user_id', $request->user()->id)->whereNull('read_at')
            ->update(['read_at' => now(), 'updated_at' => now()]);

        return response()->noContent();
    }
}
