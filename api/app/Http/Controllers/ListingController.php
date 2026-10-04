<?php

namespace App\Http\Controllers;

use App\Services\FotoUnggahan;
use App\Services\RatingToko;
use Illuminate\Database\Query\Builder;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

/**
 * Sisi konsumen modul listing: K07 Beranda, K08 Peta, K09 Filter,
 * K10 Detail tas kejutan, K11 Detail menu satuan (F-03, F-04, F-05, F-14).
 *
 * Tanpa token: isinya jualan publik, tidak ada data pribadi. Android
 * mengirim kode alergi dari profil (GET /me) lewat exclude_allergens[].
 */
class ListingController extends Controller
{
    private const KATEGORI = ['cafe', 'bakery', 'resto', 'catering', 'grocery'];

    /** GET /api/listings */
    public function daftar(Request $request): JsonResponse
    {
        $f = $request->validate([
            'type' => ['sometimes', 'array'],
            'type.*' => [Rule::in(['surprise_bag', 'menu_item'])],
            'category' => ['sometimes', Rule::in(self::KATEGORI)],
            'store_id' => ['sometimes', 'integer', 'min:1'],
            'q' => ['sometimes', 'string', 'max:80'],
            'lat' => ['required_with:lng,radius_km', 'numeric', 'between:-90,90'],
            'lng' => ['required_with:lat,radius_km', 'numeric', 'between:-180,180'],
            'radius_km' => ['sometimes', 'numeric', 'min:0.1', 'max:50'],
            'exclude_allergens' => ['sometimes', 'array', 'max:30'],
            'exclude_allergens.*' => ['string', 'max:40'],
            'halal' => ['sometimes', 'boolean'],
            'pickup_from' => ['sometimes', 'date_format:H:i'],
            'pickup_until' => ['sometimes', 'date_format:H:i'],
            'ends_within_minutes' => ['sometimes', 'integer', 'min:1', 'max:1440'],
            'per_page' => ['sometimes', 'integer', 'min:1', 'max:50'],
            // popular: bagian "Populer hari ini" di K07. Bukan iklan; urutannya
            // murni dari jumlah yang sudah dipesan.
            'sort' => ['sometimes', Rule::in(['popular'])],
        ]);

        $query = $this->yangTampil();
        $adaLokasi = isset($f['lat'], $f['lng']);

        if ($adaLokasi) {
            // ST_Distance_Sphere memakai urutan (bujur, lintang).
            $query->selectRaw(
                'ST_Distance_Sphere(POINT(stores.longitude, stores.latitude), POINT(?, ?)) / 1000 AS distance_km',
                [$f['lng'], $f['lat']],
            )->whereNotNull('stores.latitude');

            if (isset($f['radius_km'])) {
                $query->whereRaw(
                    'ST_Distance_Sphere(POINT(stores.longitude, stores.latitude), POINT(?, ?)) <= ?',
                    [$f['lng'], $f['lat'], $f['radius_km'] * 1000],
                );
            }
        }

        $query
            ->when($f['type'] ?? null, fn (Builder $q, $tipe) => $q->whereIn('listings.type', $tipe))
            ->when($f['category'] ?? null, fn (Builder $q, $k) => $q->where('stores.category', $k))
            ->when($f['store_id'] ?? null, fn (Builder $q, $toko) => $q->where('listings.store_id', $toko))
            ->when($f['q'] ?? null, fn (Builder $q, $kata) => $q->where(fn (Builder $q) => $q
                ->where('listings.title', 'like', '%'.addcslashes($kata, '%_\\').'%')
                ->orWhere('stores.name', 'like', '%'.addcslashes($kata, '%_\\').'%')))
            // Label halal hanya dari mitra: certified atau self_claim, tidak pernah ditebak.
            ->when($f['halal'] ?? false, fn (Builder $q) => $q->whereIn('listings.halal_label', ['certified', 'self_claim']))
            ->when($f['exclude_allergens'] ?? null, fn (Builder $q, $kode) => $q->whereNotExists(fn (Builder $sub) => $sub
                ->from('listing_allergens')
                ->join('allergens', 'allergens.id', '=', 'listing_allergens.allergen_id')
                ->whereColumn('listing_allergens.listing_id', 'listings.id')
                // may_contain ikut disembunyikan: untuk alergi, ragu berarti jangan.
                ->whereIn('allergens.code', $kode)))
            // Rentang jam ambil beririsan dengan jam yang dipilih di K09.
            ->when($f['pickup_from'] ?? null, fn (Builder $q, $jam) => $q->whereTime('listings.pickup_end', '>', $jam))
            ->when($f['pickup_until'] ?? null, fn (Builder $q, $jam) => $q->whereTime('listings.pickup_start', '<', $jam))
            ->when($f['ends_within_minutes'] ?? null, fn (Builder $q, $menit) => $q->where('listings.pickup_end', '<=', now()->addMinutes((int) $menit)));

        if (($f['sort'] ?? null) === 'popular') {
            // Satu listing berlaku untuk satu tanggal ambil, jadi jumlah yang sudah
            // dipesan (dipegang + terjual) sama dengan "dipesan untuk hari itu".
            // Yang belum pernah dipesan tidak dianggap populer. Kalau jumlahnya
            // sama, toko dengan rating lebih tinggi didahulukan.
            $query->whereRaw('listings.qty_reserved + listings.qty_sold > 0')
                ->whereDate('listings.pickup_date', today())
                ->orderByRaw('listings.qty_reserved + listings.qty_sold DESC')
                ->orderByRaw('COALESCE(rating.rating_avg, 0) DESC');
        }

        $adaLokasi ? $query->orderBy('distance_km') : $query->orderBy('listings.pickup_end');
        $halaman = $query->orderBy('listings.id')->paginate($f['per_page'] ?? 20)->withQueryString();

        $alergen = $this->alergenPer($halaman->pluck('id'));
        $halaman->through(fn (object $l) => $this->ringkas($l, $alergen->get($l->id, collect())));

        return response()->json($halaman);
    }

    /** GET /api/listings/{id} */
    public function detail(int $id): JsonResponse
    {
        $l = DB::table('listings')
            ->join('stores', 'stores.id', '=', 'listings.store_id')
            ->leftJoinSub(RatingToko::subquery(), 'rating', 'rating.store_id', '=', 'stores.id')
            ->where('listings.id', $id)
            ->where('listings.status', '!=', 'draft')
            ->where('stores.is_active', true)
            ->select('listings.*', 'stores.name as store_name', 'stores.category as store_category',
                'stores.address as store_address', 'stores.latitude as store_latitude',
                'stores.longitude as store_longitude', 'stores.is_temporarily_closed',
                'rating.rating_avg', 'rating.rating_count')
            ->first();

        abort_if($l === null, 404, 'Jualan tidak ditemukan.');

        $sisa = $l->qty_total - $l->qty_reserved - $l->qty_sold;
        $jamHariIni = DB::table('store_hours')
            ->where('store_id', $l->store_id)
            ->where('day_of_week', now()->dayOfWeek)
            ->first(['open_time', 'close_time', 'is_closed']);

        return response()->json(['data' => [
            ...$this->ringkas($l, $this->alergenPer(collect([$l->id]))->get($l->id, collect())),
            'description' => $l->description,
            'content_hint' => $l->content_hint,
            'ingredients_text' => $l->ingredients_text,
            'halal_certificate_no' => $l->halal_certificate_no,
            'status' => $l->status,
            'is_available' => $l->status === 'active' && $sisa > 0 && ! $l->is_temporarily_closed
                && Carbon::parse($l->pickup_end)->isFuture(),
            'items' => DB::table('listing_items')->where('listing_id', $l->id)->orderBy('id')
                ->get(['label', 'qty', 'weight_gram', 'unit_value_rupiah']),
            'store' => [
                'id' => $l->store_id,
                'name' => $l->store_name,
                'category' => $l->store_category,
                'address' => $l->store_address,
                'latitude' => $l->store_latitude === null ? null : (float) $l->store_latitude,
                'longitude' => $l->store_longitude === null ? null : (float) $l->store_longitude,
                'hours_today' => $jamHariIni,
                ...RatingToko::format($l->rating_avg, $l->rating_count),
            ],
        ]]);
    }

    /**
     * Yang boleh tampil di beranda: aktif, stok masih ada, belum lewat jam
     * ambil, dan tokonya buka.
     */
    private function yangTampil(): Builder
    {
        return DB::table('listings')
            ->join('stores', 'stores.id', '=', 'listings.store_id')
            ->leftJoinSub(RatingToko::subquery(), 'rating', 'rating.store_id', '=', 'stores.id')
            ->select('listings.*', 'stores.name as store_name', 'stores.category as store_category',
                'rating.rating_avg', 'rating.rating_count')
            ->where('listings.status', 'active')
            ->where('listings.pickup_end', '>', now())
            ->whereRaw('listings.qty_total > listings.qty_reserved + listings.qty_sold')
            ->where('stores.is_active', true)
            ->where('stores.is_temporarily_closed', false);
    }

    /**
     * @param  Collection<int, int>  $idListing
     * @return Collection<int, Collection<int, object>>
     */
    private function alergenPer(Collection $idListing): Collection
    {
        return DB::table('listing_allergens')
            ->join('allergens', 'allergens.id', '=', 'listing_allergens.allergen_id')
            ->whereIn('listing_allergens.listing_id', $idListing)
            ->orderBy('allergens.sort_order')
            ->get(['listing_allergens.listing_id', 'allergens.code', 'allergens.name', 'listing_allergens.presence'])
            ->groupBy('listing_id')
            ->map(fn (Collection $baris) => $baris->map(fn ($a) => ['code' => $a->code, 'name' => $a->name, 'presence' => $a->presence])->values());
    }

    private function ringkas(object $l, Collection $alergen): array
    {
        $akhir = Carbon::parse($l->pickup_end);

        return [
            'id' => $l->id,
            'type' => $l->type,
            'title' => $l->title,
            'photo_url' => FotoUnggahan::url($l->photo_path),
            'price_rupiah' => $l->price_rupiah,
            'original_value_rupiah' => $l->original_value_rupiah,
            'qty_remaining' => max(0, $l->qty_total - $l->qty_reserved - $l->qty_sold),
            // Label "5 dipesan" di bagian Populer hari ini.
            'qty_ordered' => $l->qty_reserved + $l->qty_sold,
            'pickup_start' => Carbon::parse($l->pickup_start)->toIso8601String(),
            'pickup_end' => $akhir->toIso8601String(),
            // Label "48 menit" di kartu K07. 0 kalau sudah lewat.
            'minutes_until_end' => max(0, (int) floor(now()->diffInMinutes($akhir, false))),
            'halal_label' => $l->halal_label,
            'allergens' => $alergen,
            'distance_km' => isset($l->distance_km) ? round((float) $l->distance_km, 2) : null,
            'store' => [
                'id' => $l->store_id,
                'name' => $l->store_name,
                'category' => $l->store_category,
                ...RatingToko::format($l->rating_avg, $l->rating_count),
            ],
        ];
    }
}
