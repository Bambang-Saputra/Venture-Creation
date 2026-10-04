<?php

namespace App\Http\Controllers\Partner;

use App\Http\Controllers\Controller;
use App\Services\FotoUnggahan;
use App\Services\Notifikasi;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

/**
 * Sisi mitra modul listing (F-09): M09 Pasang tas, M10 Kelola jualan,
 * M16 Pasang menu satuan, M17 Kelola menu satuan.
 *
 * Hanya pemilik toko yang boleh. Kasir (store_members.role = cashier)
 * hanya mencatat sisa dan mencocokkan kode pickup.
 */
class ListingMitraController extends Controller
{
    use AksesToko;

    private const HALAL = ['certified', 'self_claim', 'not_stated'];

    /** GET /api/partner/stores/{store}/templates (pilihan template di M09) */
    public function template(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);

        return response()->json(['data' => DB::table('surprise_bag_templates')
            ->where('store_id', $store)->where('is_active', true)->orderBy('name')
            ->get(['id', 'name', 'content_hint', 'price_rupiah', 'original_value_rupiah', 'default_qty',
                'pickup_start_time', 'pickup_end_time', 'halal_label', 'photo_path'])
            ->map(fn (object $t) => [...(array) $t, 'photo_url' => FotoUnggahan::url($t->photo_path)])]);
    }

    /** GET /api/partner/stores/{store}/products ("Tambah item dari menu toko" di M16) */
    public function produk(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);

        return response()->json(['data' => DB::table('products')
            ->where('store_id', $store)->where('is_active', true)->orderBy('name')
            ->get(['id', 'name', 'unit', 'price_rupiah', 'ingredients_text'])]);
    }

    /** GET /api/partner/stores/{store}/listings?type=&status=&date= (M10, M17) */
    public function daftar(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);
        $f = $request->validate([
            'type' => ['sometimes', Rule::in(['surprise_bag', 'menu_item'])],
            'status' => ['sometimes', Rule::in(['draft', 'active', 'paused', 'sold_out', 'expired'])],
            'date' => ['sometimes', 'date_format:Y-m-d'],
        ]);

        $baris = DB::table('listings')
            ->where('store_id', $store)
            ->where('pickup_date', $f['date'] ?? now()->toDateString())
            ->when($f['type'] ?? null, fn ($q, $t) => $q->where('type', $t))
            ->when($f['status'] ?? null, fn ($q, $s) => $q->where('status', $s))
            ->orderBy('pickup_start')->orderBy('id')
            ->get();

        return response()->json(['data' => $this->bentuk($baris)]);
    }

    /**
     * POST /api/partner/stores/{store}/listings
     *
     * surprise_bag: satu listing, nilai awal dari template bila ada.
     * menu_item: satu listing per item di `items`, dibuat sekaligus.
     * `publish` (bawaan true) = "Terbitkan" dalam satu ketukan.
     */
    public function buat(Request $request, int $store): JsonResponse
    {
        $toko = $this->tokoMilik($request, $store);
        $tas = $request->input('type') === 'surprise_bag';

        $data = $request->validate([
            'type' => ['required', Rule::in(['surprise_bag', 'menu_item'])],
            'pickup_start' => ['required', 'date_format:H:i'],
            'pickup_end' => ['required', 'date_format:H:i', 'after:pickup_start'],
            'publish' => ['sometimes', 'boolean'],
            'halal_label' => ['sometimes', Rule::in(self::HALAL)],
            ...($tas ? [
                'template_id' => ['nullable', 'integer', Rule::exists('surprise_bag_templates', 'id')->where('store_id', $store)->where('is_active', true)],
                'title' => ['required_without:template_id', 'nullable', 'string', 'max:140'],
                'description' => ['nullable', 'string', 'max:1000'],
                'content_hint' => ['nullable', 'string', 'max:500'],
                'price_rupiah' => ['required_without:template_id', 'nullable', 'integer', 'min:1000', 'max:1000000'],
                'original_value_rupiah' => ['nullable', 'integer', 'gte:price_rupiah', 'max:5000000'],
                'qty_total' => ['required', 'integer', 'min:1', 'max:200'],
                'ingredients_text' => ['nullable', 'string', 'max:2000'],
                ...$this->aturanAlergen('allergens'),
            ] : [
                'items' => ['required', 'array', 'min:1', 'max:30'],
                'items.*.product_id' => ['required', 'integer', 'distinct', Rule::exists('products', 'id')->where('store_id', $store)->where('is_active', true)],
                'items.*.qty_total' => ['required', 'integer', 'min:1', 'max:200'],
                'items.*.price_rupiah' => ['required', 'integer', 'min:500', 'max:1000000'],
                'items.*.original_value_rupiah' => ['nullable', 'integer', 'max:5000000'],
                'items.*.ingredients_text' => ['nullable', 'string', 'max:2000'],
                ...$this->aturanAlergen('items.*.allergens'),
            ]),
        ]);

        $mulai = now()->setTimeFromTimeString($data['pickup_start']);
        $akhir = now()->setTimeFromTimeString($data['pickup_end']);
        $terbit = $data['publish'] ?? true;

        if ($terbit && $akhir->isPast()) {
            throw ValidationException::withMessages(['pickup_end' => 'Jam ambil terakhir sudah lewat. Pilih jam yang masih akan datang.']);
        }

        $umum = [
            'store_id' => $store,
            'pickup_date' => now()->toDateString(),
            'pickup_start' => $mulai,
            'pickup_end' => $akhir,
            'status' => $terbit ? 'active' : 'draft',
            'published_at' => $terbit ? now() : null,
            'created_at' => now(),
            'updated_at' => now(),
        ];

        $draf = $tas
            ? [$this->drafTas($data, $toko)]
            : $this->drafMenu($data, $toko);

        if ($terbit) {
            foreach ($draf as $i => [$kolom]) {
                if (mb_strlen(trim($kolom['ingredients_text'] ?? '')) < 3) {
                    throw ValidationException::withMessages([
                        ($tas ? 'ingredients_text' : "items.{$i}.ingredients_text") => 'Isi kandungan wajib diisi sebelum terbit, supaya pembeli yang alergi bisa memeriksa.',
                    ]);
                }
            }
        }

        $idBaru = DB::transaction(function () use ($draf, $umum) {
            $id = [];
            foreach ($draf as [$kolom, $alergen]) {
                $idListing = DB::table('listings')->insertGetId([...$umum, ...$kolom]);
                $this->simpanAlergen($idListing, $alergen);
                $id[] = $idListing;
            }

            return $id;
        });

        if ($terbit) {
            foreach ($idBaru as $id) {
                app(Notifikasi::class)->mitraFavoritMemasang($id);
            }
        }

        return response()->json(['data' => $this->bentuk(DB::table('listings')->whereIn('id', $idBaru)->orderBy('id')->get())], 201);
    }

    /** POST /api/partner/stores/{store}/listings/{listing}/publish (draf atau jeda -> aktif) */
    public function terbitkan(Request $request, int $store, int $listing): JsonResponse
    {
        $this->tokoMilik($request, $store);

        $respons = $this->ubahStatus($store, $listing, ['draft', 'paused'], function (object $l) {
            if (Carbon::parse($l->pickup_end)->isPast()) {
                throw ValidationException::withMessages(['pickup_end' => 'Jam ambil sudah lewat. Pasang jualan baru untuk hari ini.']);
            }
            if (mb_strlen(trim($l->ingredients_text ?? '')) < 3) {
                throw ValidationException::withMessages(['ingredients_text' => 'Isi kandungan wajib diisi sebelum terbit.']);
            }

            return ['status' => 'active', 'published_at' => $l->published_at ?? now()];
        });

        // Dibatasi satu per toko per orang per hari, jadi jeda lalu terbit ulang tidak membanjiri.
        app(Notifikasi::class)->mitraFavoritMemasang($listing);

        return $respons;
    }

    /** POST /api/partner/stores/{store}/listings/{listing}/pause (aktif -> jeda) */
    public function jeda(Request $request, int $store, int $listing): JsonResponse
    {
        $this->tokoMilik($request, $store);

        // Pesanan yang sudah dibuat tetap berlaku; jeda hanya menghentikan pesanan baru.
        return $this->ubahStatus($store, $listing, ['active'], fn () => ['status' => 'paused']);
    }

    private function ubahStatus(int $store, int $listing, array $dariStatus, callable $ubah): JsonResponse
    {
        $hasil = DB::transaction(function () use ($store, $listing, $dariStatus, $ubah) {
            $l = DB::table('listings')->where('id', $listing)->where('store_id', $store)->lockForUpdate()->first();
            abort_if($l === null, 404, 'Jualan tidak ditemukan.');

            if (! in_array($l->status, $dariStatus, true)) {
                abort(409, "Jualan berstatus {$l->status} tidak bisa diubah dengan aksi ini.");
            }

            DB::table('listings')->where('id', $l->id)->update([...$ubah($l), 'updated_at' => now()]);

            return DB::table('listings')->where('id', $l->id)->get();
        });

        return response()->json(['data' => $this->bentuk($hasil)->first()]);
    }

    /** @return array{0: array<string, mixed>, 1: array<int, array{code: string, presence?: string}>} */
    private function drafTas(array $data, object $toko): array
    {
        $t = isset($data['template_id'])
            ? DB::table('surprise_bag_templates')->where('id', $data['template_id'])->first()
            : null;
        $halal = $data['halal_label'] ?? $t?->halal_label ?? $toko->halal_label;

        return [[
            'type' => 'surprise_bag',
            'template_id' => $t?->id,
            'title' => $data['title'] ?? $t->name,
            'description' => $data['description'] ?? $t?->description,
            'content_hint' => $data['content_hint'] ?? $t?->content_hint,
            'photo_path' => $t?->photo_path,
            'price_rupiah' => $data['price_rupiah'] ?? $t->price_rupiah,
            'original_value_rupiah' => $data['original_value_rupiah'] ?? $t?->original_value_rupiah,
            'qty_total' => $data['qty_total'],
            'ingredients_text' => $data['ingredients_text'] ?? $t?->ingredients_text ?? $toko->default_ingredients_text,
            'halal_label' => $halal,
            'halal_certificate_no' => $halal === 'certified' ? $toko->halal_certificate_no : null,
        ], $data['allergens'] ?? []];
    }

    /** @return array<int, array{0: array<string, mixed>, 1: array}> */
    private function drafMenu(array $data, object $toko): array
    {
        $produk = DB::table('products')->whereIn('id', array_column($data['items'], 'product_id'))->get()->keyBy('id');
        $halal = $data['halal_label'] ?? $toko->halal_label;

        return array_map(fn (array $item) => [[
            'type' => 'menu_item',
            'product_id' => $item['product_id'],
            'title' => $produk[$item['product_id']]->name,
            'price_rupiah' => $item['price_rupiah'],
            // Harga normal produk jadi harga coret bila mitra tidak mengisinya.
            'original_value_rupiah' => $item['original_value_rupiah'] ?? ($produk[$item['product_id']]->price_rupiah ?: null),
            'qty_total' => $item['qty_total'],
            'ingredients_text' => $item['ingredients_text'] ?? $produk[$item['product_id']]->ingredients_text ?? $toko->default_ingredients_text,
            'halal_label' => $halal,
            'halal_certificate_no' => $halal === 'certified' ? $toko->halal_certificate_no : null,
        ], $item['allergens'] ?? []], $data['items']);
    }

    private function aturanAlergen(string $kunci): array
    {
        return [
            $kunci => ['sometimes', 'array', 'max:30'],
            "{$kunci}.*.code" => ['required', 'string', Rule::exists('allergens', 'code')->where('is_active', true)],
            "{$kunci}.*.presence" => ['sometimes', Rule::in(['contains', 'may_contain'])],
        ];
    }

    private function simpanAlergen(int $listing, array $alergen): void
    {
        if ($alergen === []) {
            return;
        }

        $id = DB::table('allergens')->whereIn('code', array_column($alergen, 'code'))->pluck('id', 'code');
        $baris = collect($alergen)->keyBy('code')->map(fn (array $a) => [
            'listing_id' => $listing,
            'allergen_id' => $id[$a['code']],
            'presence' => $a['presence'] ?? 'contains',
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        DB::table('listing_allergens')->insert($baris->values()->all());
    }

    /**
     * @param  Collection<int, object>  $baris
     * @return Collection<int, array<string, mixed>>
     */
    private function bentuk(Collection $baris): Collection
    {
        $alergen = DB::table('listing_allergens')
            ->join('allergens', 'allergens.id', '=', 'listing_allergens.allergen_id')
            ->whereIn('listing_id', $baris->pluck('id'))
            ->orderBy('allergens.sort_order')
            ->get(['listing_id', 'allergens.code', 'listing_allergens.presence'])
            ->groupBy('listing_id');

        return $baris->map(fn (object $l) => [
            'id' => $l->id,
            'type' => $l->type,
            'status' => $l->status,
            'title' => $l->title,
            'product_id' => $l->product_id,
            'template_id' => $l->template_id,
            'photo_url' => FotoUnggahan::url($l->photo_path),
            'price_rupiah' => $l->price_rupiah,
            'original_value_rupiah' => $l->original_value_rupiah,
            'qty_total' => $l->qty_total,
            'qty_reserved' => $l->qty_reserved,
            'qty_sold' => $l->qty_sold,
            'qty_remaining' => max(0, $l->qty_total - $l->qty_reserved - $l->qty_sold),
            // "Potensi pemasukan" di M09 dan M16.
            'potential_income_rupiah' => $l->price_rupiah * $l->qty_total,
            'pickup_start' => Carbon::parse($l->pickup_start)->toIso8601String(),
            'pickup_end' => Carbon::parse($l->pickup_end)->toIso8601String(),
            'ingredients_text' => $l->ingredients_text,
            'halal_label' => $l->halal_label,
            'published_at' => $l->published_at === null ? null : Carbon::parse($l->published_at)->toIso8601String(),
            'allergens' => ($alergen[$l->id] ?? collect())->map(fn ($a) => ['code' => $a->code, 'presence' => $a->presence])->values(),
        ])->values();
    }
}
