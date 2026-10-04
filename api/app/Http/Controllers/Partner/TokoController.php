<?php

namespace App\Http\Controllers\Partner;

use App\Http\Controllers\Controller;
use App\Services\FotoUnggahan;
use App\Services\RatingToko;
use App\Services\RingkasanSisa;
use App\Support\NomorHp;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

/**
 * M14 Profil toko (F-16) dan M13 Saldo (F-17).
 */
class TokoController extends Controller
{
    use AksesToko;

    public function __construct(private readonly RingkasanSisa $ringkasan) {}

    /** GET /api/partner/stores: toko yang bisa dibuka akun ini, untuk memilih toko setelah M01/M02. */
    public function daftar(Request $request): JsonResponse
    {
        $user = $request->user();
        $anggota = DB::table('store_members')->where('user_id', $user->id)->whereNull('revoked_at')->pluck('role', 'store_id');

        $toko = DB::table('stores')->where('owner_user_id', $user->id)->orWhereIn('id', $anggota->keys())
            ->orderBy('name')->get(['id', 'name', 'category', 'address', 'photo_path', 'owner_user_id', 'is_temporarily_closed']);

        return response()->json(['data' => $toko->map(fn (object $t) => [
            'id' => $t->id,
            'name' => $t->name,
            'category' => $t->category,
            'address' => $t->address,
            'photo_path' => $t->photo_path,
            'photo_url' => FotoUnggahan::url($t->photo_path),
            'is_temporarily_closed' => (bool) $t->is_temporarily_closed,
            'my_role' => (int) $t->owner_user_id === $user->id ? 'owner' : $anggota[$t->id],
        ])]);
    }

    /** GET /api/partner/stores/{store}. Kasir boleh melihat, tapi saldo disembunyikan. */
    public function tampil(Request $request, int $store): JsonResponse
    {
        $toko = $this->tokoMilik($request, $store, pemilikSaja: false);

        return response()->json(['data' => $this->bentuk($request, $toko)]);
    }

    /**
     * PUT/PATCH /api/partner/stores/{store}, hanya pemilik.
     * Tiap field hanya diproses kalau dikirim, jadi tombol "Ubah" jam operasional
     * dan sakelar "Tutup sementara" cukup mengirim bagiannya sendiri.
     */
    public function ubah(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);

        $f = $request->validate([
            'name' => ['sometimes', 'string', 'min:2', 'max:140'],
            'category' => ['sometimes', Rule::in(['cafe', 'bakery', 'resto', 'catering', 'grocery'])],
            'address' => ['sometimes', 'string', 'min:5', 'max:255'],
            'latitude' => ['required_with:longitude', 'nullable', 'numeric', 'between:-90,90'],
            'longitude' => ['required_with:latitude', 'nullable', 'numeric', 'between:-180,180'],
            'whatsapp' => ['sometimes', 'nullable', 'string', 'max:20'],
            'halal_label' => ['sometimes', Rule::in(['certified', 'self_claim', 'not_stated'])],
            'halal_certificate_no' => ['sometimes', 'nullable', 'string', 'max:80'],
            'default_ingredients_text' => ['sometimes', 'nullable', 'string', 'max:2000'],
            'is_temporarily_closed' => ['sometimes', 'boolean'],
            'hours' => ['sometimes', 'array', 'min:1', 'max:7'],
            'hours.*.day_of_week' => ['required', 'integer', 'between:0,6', 'distinct'],
            'hours.*.is_closed' => ['sometimes', 'boolean'],
            'hours.*.open_time' => ['exclude_if:hours.*.is_closed,true', 'required', 'date_format:H:i'],
            'hours.*.close_time' => ['exclude_if:hours.*.is_closed,true', 'required', 'date_format:H:i', 'after:hours.*.open_time'],
        ]);

        if (array_key_exists('whatsapp', $f) && $f['whatsapp'] !== null) {
            $f['whatsapp'] = NomorHp::normalisasi($f['whatsapp'])
                ?? throw ValidationException::withMessages(['whatsapp' => 'Nomor WhatsApp tidak valid.']);
        }

        // Label "certified" tanpa nomor sertifikat sama saja dengan menulis "halal" tanpa dasar.
        $label = $f['halal_label'] ?? DB::table('stores')->where('id', $store)->value('halal_label');
        $sertifikat = array_key_exists('halal_certificate_no', $f) ? $f['halal_certificate_no']
            : DB::table('stores')->where('id', $store)->value('halal_certificate_no');
        if ($label === 'certified' && blank($sertifikat)) {
            throw ValidationException::withMessages(['halal_certificate_no' => 'Nomor sertifikat halal wajib diisi untuk label bersertifikat.']);
        }

        DB::transaction(function () use ($request, $store, $f) {
            $kolom = collect($f)->except('hours')->all();
            if ($kolom !== []) {
                DB::table('stores')->where('id', $store)->update($kolom + ['updated_at' => now()]);
            }

            foreach ($f['hours'] ?? [] as $h) {
                $tutup = (bool) ($h['is_closed'] ?? false);
                DB::table('store_hours')->updateOrInsert(
                    ['store_id' => $store, 'day_of_week' => $h['day_of_week']],
                    ['is_closed' => $tutup, 'open_time' => $tutup ? null : $h['open_time'], 'close_time' => $tutup ? null : $h['close_time'],
                        'updated_at' => now(), 'created_at' => now()],
                );
            }

            DB::table('audit_logs')->insert([
                'user_id' => $request->user()->id, 'store_id' => $store, 'action' => 'store.update',
                'subject_type' => 'store', 'subject_id' => $store,
                'meta' => json_encode(['fields' => array_keys($f)]),
                'ip' => $request->ip(), 'created_at' => now(), 'updated_at' => now(),
            ]);
        });

        $toko = DB::table('stores')->where('id', $store)->first();

        return response()->json(['data' => $this->bentuk($request, $toko)]);
    }

    /** GET /api/partner/stores/{store}/balance (M13), hanya pemilik. */
    public function saldo(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);
        $saldo = DB::table('store_balances')->where('store_id', $store)->first();
        $minggu = $this->ringkasan->rentang($store, now()->startOfWeek(Carbon::MONDAY), now());

        return response()->json(['data' => [
            'available_rupiah' => (int) ($saldo->available_rupiah ?? 0),
            'pending_rupiah' => (int) ($saldo->pending_rupiah ?? 0),
            'lifetime_rupiah' => (int) ($saldo->lifetime_rupiah ?? 0),
            // "Dari 62 tas minggu ini"
            'items_sold_this_week' => $minggu['items_sold'],
            // M13/M20: tim tidak memegang uang mitra selama pilot, pembayaran di tempat (ADR-0004).
            'withdrawal' => ['enabled' => false, 'reason' => 'Pencairan tersedia setelah masa uji coba.'],
        ]]);
    }

    /** GET /api/partner/stores/{store}/balance/transactions (Riwayat di M13), hanya pemilik. */
    public function transaksi(Request $request, int $store): JsonResponse
    {
        $this->tokoMilik($request, $store);

        $halaman = DB::table('balance_transactions as t')
            ->leftJoin('pickup_codes as p', 'p.order_id', '=', 't.order_id')
            ->where('t.store_id', $store)
            ->orderByDesc('t.created_at')->orderByDesc('t.id')
            ->select('t.id', 't.type', 't.amount_rupiah', 't.balance_after_rupiah', 't.description', 't.created_at', 'p.code as pickup_code')
            ->selectSub(fn ($q) => $q->from('order_items')->whereColumn('order_id', 't.order_id')->orderBy('id')->limit(1)->select('title_snapshot'), 'title')
            ->selectSub(fn ($q) => $q->from('order_items')->whereColumn('order_id', 't.order_id')->selectRaw('COUNT(*)'), 'line_count')
            ->paginate(20);

        $halaman->through(fn (object $t) => [
            'id' => $t->id,
            'type' => $t->type,
            'amount_rupiah' => (int) $t->amount_rupiah,
            'balance_after_rupiah' => (int) $t->balance_after_rupiah,
            // "Tas Pastry Sore · LF7Q2K". Pesanan berisi beberapa item memakai "+N lainnya".
            'title' => $t->title === null ? $t->description
                : $t->title.((int) $t->line_count > 1 ? ' +'.((int) $t->line_count - 1).' lainnya' : ''),
            'pickup_code' => $t->pickup_code,
            'description' => $t->description,
            'created_at' => Carbon::parse($t->created_at)->toIso8601String(),
        ]);

        return response()->json($halaman);
    }

    private function bentuk(Request $request, object $toko): array
    {
        $user = $request->user();
        $peran = (int) $toko->owner_user_id === $user->id ? 'owner'
            : DB::table('store_members')->where('store_id', $toko->id)->where('user_id', $user->id)->whereNull('revoked_at')->value('role');

        $jam = DB::table('store_hours')->where('store_id', $toko->id)->orderBy('day_of_week')->get();

        return [
            'id' => $toko->id,
            'name' => $toko->name,
            'slug' => $toko->slug,
            'category' => $toko->category,
            'address' => $toko->address,
            'latitude' => $toko->latitude === null ? null : (float) $toko->latitude,
            'longitude' => $toko->longitude === null ? null : (float) $toko->longitude,
            'whatsapp' => $toko->whatsapp,
            'photo_path' => $toko->photo_path,
            'photo_url' => FotoUnggahan::url($toko->photo_path),
            'halal_label' => $toko->halal_label,
            'halal_certificate_no' => $toko->halal_certificate_no,
            'default_ingredients_text' => $toko->default_ingredients_text,
            'is_temporarily_closed' => (bool) $toko->is_temporarily_closed,
            'is_pilot_partner' => $toko->pilot_consent_at !== null,
            // Chip "4,8 (180)" di M14.
            ...RatingToko::untukToko($toko->id),
            'hours' => $jam->map(fn (object $h) => [
                'day_of_week' => (int) $h->day_of_week,
                'is_closed' => (bool) $h->is_closed,
                'open_time' => $h->open_time === null ? null : substr($h->open_time, 0, 5),
                'close_time' => $h->close_time === null ? null : substr($h->close_time, 0, 5),
            ])->values(),
            'my_role' => $peran,
            // Kasir tidak boleh melihat saldo (M15).
            'available_balance_rupiah' => $peran === 'owner'
                ? (int) DB::table('store_balances')->where('store_id', $toko->id)->value('available_rupiah')
                : null,
        ];
    }
}
