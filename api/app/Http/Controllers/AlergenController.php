<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

/**
 * K05 Alergi dan pantangan (F-02).
 *
 * Android merender chip dari GET /allergens, bukan dari daftar tetap di
 * aplikasi, supaya menambah alergen cukup lewat SeederAlergen.
 * Pilihan diacu lewat `code`, bukan id, karena code sama di semua database.
 */
class AlergenController extends Controller
{
    /** GET /api/allergens */
    public function daftar(): JsonResponse
    {
        $alergen = DB::table('allergens')
            ->where('is_active', true)
            ->orderBy('sort_order')
            ->get(['code', 'name', 'type']);

        return response()->json(['data' => $alergen]);
    }

    /**
     * PUT /api/me/allergens. Mengganti seluruh pilihan: kirim [] untuk
     * mengosongkan. Tombol "Lewati" di K05 tidak perlu memanggil ini.
     */
    public function gantiMilikSaya(Request $request): JsonResponse
    {
        $user = $request->user();

        if ($user->role !== 'consumer') {
            return response()->json(['message' => 'Alergi dan pantangan hanya untuk akun konsumen.'], 403);
        }

        $data = $request->validate([
            'allergens' => ['present', 'array', 'max:50'],
            'allergens.*.code' => [
                'required', 'string', 'distinct',
                Rule::exists('allergens', 'code')->where('is_active', true),
            ],
            'allergens.*.severity' => ['sometimes', Rule::in(['avoid', 'severe'])],
        ]);

        $pilihan = collect($data['allergens']);
        $idPerKode = DB::table('allergens')->whereIn('code', $pilihan->pluck('code'))->pluck('id', 'code');

        DB::transaction(function () use ($user, $pilihan, $idPerKode) {
            DB::table('user_allergens')->where('user_id', $user->id)->delete();
            DB::table('user_allergens')->insert($pilihan->map(fn (array $baris) => [
                'user_id' => $user->id,
                'allergen_id' => $idPerKode[$baris['code']],
                'severity' => $baris['severity'] ?? 'avoid',
                'created_at' => now(),
                'updated_at' => now(),
            ])->all());
        });

        return response()->json(['data' => self::milik($user->id)]);
    }

    /**
     * Juga dipakai GET /me, supaya K05 dan K19 bisa menandai chip yang sudah dipilih.
     *
     * @return Collection<int, object>
     */
    public static function milik(int $userId): Collection
    {
        return DB::table('user_allergens')
            ->join('allergens', 'allergens.id', '=', 'user_allergens.allergen_id')
            ->where('user_allergens.user_id', $userId)
            ->orderBy('allergens.sort_order')
            ->get(['allergens.code', 'allergens.name', 'allergens.type', 'user_allergens.severity']);
    }
}
