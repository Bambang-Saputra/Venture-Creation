<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\DB;

/**
 * K16 Favorit (F-22), tab Mitra. Tab "Tas" belum didukung: tabel
 * favorites hanya menyimpan toko, dan jualan berganti setiap hari.
 */
class FavoritController extends Controller
{
    /** GET /api/favorites?lat=&lng= */
    public function daftar(Request $request): JsonResponse
    {
        $user = $this->konsumen($request);
        $f = $request->validate([
            'lat' => ['required_with:lng', 'numeric', 'between:-90,90'],
            'lng' => ['required_with:lat', 'numeric', 'between:-180,180'],
        ]);
        $adaLokasi = isset($f['lat'], $f['lng']);

        $toko = DB::table('favorites')->join('stores', 'stores.id', '=', 'favorites.store_id')
            ->leftJoin('store_hours', fn ($j) => $j->on('store_hours.store_id', '=', 'stores.id')->where('store_hours.day_of_week', now()->dayOfWeek))
            ->where('favorites.user_id', $user->id)->where('stores.is_active', true)
            ->select('stores.id', 'stores.name', 'stores.category', 'stores.photo_path', 'stores.is_temporarily_closed',
                'store_hours.close_time', 'store_hours.is_closed', 'favorites.created_at')
            ->when($adaLokasi, fn ($q) => $q->selectRaw(
                'CASE WHEN stores.latitude IS NULL THEN NULL ELSE ST_Distance_Sphere(POINT(stores.longitude, stores.latitude), POINT(?, ?)) / 1000 END AS distance_km',
                [$f['lng'], $f['lat']],
            ))
            ->orderByDesc('favorites.created_at')->get();

        $id = $toko->pluck('id');
        // Stok yang masih bisa dipesan hari ini, dengan syarat yang sama dengan beranda.
        $stok = DB::table('listings')->whereIn('store_id', $id)->where('status', 'active')->where('pickup_end', '>', now())
            ->whereRaw('qty_total > qty_reserved + qty_sold')
            ->groupBy('store_id', 'type')->selectRaw('store_id, type, SUM(qty_total - qty_reserved - qty_sold) AS sisa')
            ->get()->groupBy('store_id');
        // "Biasanya pasang jam 19.00": jam terbit yang paling sering selama 4 minggu terakhir.
        $jamBiasa = DB::table('listings')->whereIn('store_id', $id)->whereNotNull('published_at')
            ->where('published_at', '>=', now()->subWeeks(4))
            ->groupBy('store_id', DB::raw('HOUR(published_at)'))
            ->selectRaw('store_id, HOUR(published_at) AS jam, COUNT(*) AS n')
            ->orderByDesc('n')->orderBy('jam')->get()->unique('store_id')->keyBy('store_id');

        return response()->json(['data' => $toko->map(function (object $t) use ($stok, $jamBiasa) {
            $s = $stok->get($t->id, collect())->keyBy('type');
            $tutupHariIni = $t->is_temporarily_closed || $t->is_closed;

            return [
                'id' => $t->id,
                'name' => $t->name,
                'category' => $t->category,
                'photo_path' => $t->photo_path,
                'distance_km' => isset($t->distance_km) ? round((float) $t->distance_km, 2) : null,
                'closes_at' => $tutupHariIni || $t->close_time === null ? null : substr($t->close_time, 0, 5),
                'is_temporarily_closed' => (bool) $t->is_temporarily_closed,
                // Chip "2 tas tersedia" / "Menu satuan tersedia" / "Belum ada tas hari ini".
                'available_bags' => (int) ($s->get('surprise_bag')->sisa ?? 0),
                'has_menu_available' => (int) ($s->get('menu_item')->sisa ?? 0) > 0,
                'usual_publish_time' => ($j = $jamBiasa->get($t->id)) === null ? null : sprintf('%02d:00', $j->jam),
            ];
        })->values()]);
    }

    /** POST /api/favorites  { store_id }. Mengirim ulang tidak menggandakan. */
    public function tambah(Request $request): JsonResponse
    {
        $user = $this->konsumen($request);
        $data = $request->validate(['store_id' => ['required', 'integer']]);
        abort_unless(DB::table('stores')->where('id', $data['store_id'])->where('is_active', true)->exists(), 404, 'Toko tidak ditemukan.');

        $baru = DB::table('favorites')->insertOrIgnore([
            'user_id' => $user->id, 'store_id' => $data['store_id'], 'created_at' => now(), 'updated_at' => now(),
        ]);

        return response()->json(['data' => ['store_id' => $data['store_id'], 'is_favorite' => true]], $baru ? 201 : 200);
    }

    /** DELETE /api/favorites/{store} */
    public function hapus(Request $request, int $store): Response
    {
        $user = $this->konsumen($request);
        DB::table('favorites')->where('user_id', $user->id)->where('store_id', $store)->delete();

        return response()->noContent();
    }

    private function konsumen(Request $request): object
    {
        abort_if($request->user()->role !== 'consumer', 403, 'Favorit hanya untuk akun konsumen.');

        return $request->user();
    }
}
