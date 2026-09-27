<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * M13 Saldo, M14 Profil toko, M15 Pengaturan toko dan kasir.
 */
class TokoTest extends TestCase
{
    use RefreshDatabase;

    private User $pemilik;

    private User $kasir;

    private int $toko;

    protected function setUp(): void
    {
        parent::setUp();

        $this->travelTo('2026-09-18 19:00:00');
        $this->pemilik = User::forceCreate(['phone' => '6281299887766', 'name' => 'Kalyan Pratama', 'role' => 'partner'])->refresh();
        $this->kasir = User::forceCreate(['phone' => '6281322445566', 'name' => 'Rina', 'role' => 'partner'])->refresh();
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $this->pemilik->id, 'name' => 'Kopi Kalyan SCBD', 'slug' => 'kopi-kalyan', 'category' => 'cafe',
            'address' => 'Jl. Jend. Sudirman Kav 52',
        ]);
        DB::table('store_members')->insert(['store_id' => $this->toko, 'user_id' => $this->kasir->id, 'role' => 'cashier', 'invited_at' => now()]);
        DB::table('store_balances')->insert(['store_id' => $this->toko, 'available_rupiah' => 1186000, 'pending_rupiah' => 18000, 'lifetime_rupiah' => 2086000]);
    }

    public function test_daftar_toko_menyebut_peran(): void
    {
        $this->sebagai($this->pemilik)->getJson('/api/partner/stores')->assertOk()->assertJsonPath('data.0.my_role', 'owner');
        $this->sebagai($this->kasir)->getJson('/api/partner/stores')->assertOk()->assertJsonPath('data.0.my_role', 'cashier');
    }

    public function test_kasir_bisa_melihat_toko_tanpa_saldo(): void
    {
        $this->sebagai($this->pemilik)->getJson($this->url())->assertOk()->assertJsonPath('data.available_balance_rupiah', 1186000);
        $this->sebagai($this->kasir)->getJson($this->url())->assertOk()
            ->assertJsonPath('data.name', 'Kopi Kalyan SCBD')
            ->assertJsonPath('data.available_balance_rupiah', null);
    }

    public function test_pemilik_ubah_jam_dan_tutup_sementara(): void
    {
        $this->sebagai($this->pemilik)->patchJson($this->url(), [
            'is_temporarily_closed' => true,
            'hours' => [
                ['day_of_week' => 1, 'open_time' => '07:00', 'close_time' => '21:00'],
                ['day_of_week' => 0, 'is_closed' => true],
            ],
        ])->assertOk()
            ->assertJsonPath('data.is_temporarily_closed', true)
            ->assertJsonPath('data.hours.0', ['day_of_week' => 0, 'is_closed' => true, 'open_time' => null, 'close_time' => null])
            ->assertJsonPath('data.hours.1.close_time', '21:00');

        $this->assertDatabaseHas('audit_logs', ['store_id' => $this->toko, 'action' => 'store.update']);
    }

    public function test_validasi_ubah_toko(): void
    {
        $this->sebagai($this->pemilik)->patchJson($this->url(), [
            'hours' => [['day_of_week' => 2, 'open_time' => '21:00', 'close_time' => '07:00']],
        ])->assertStatus(422)->assertJsonValidationErrors('hours.0.close_time');

        // Label bersertifikat wajib punya nomor sertifikat.
        $this->sebagai($this->pemilik)->patchJson($this->url(), ['halal_label' => 'certified'])
            ->assertStatus(422)->assertJsonValidationErrors('halal_certificate_no');

        $this->sebagai($this->pemilik)->putJson($this->url(), ['whatsapp' => '0812-9988-7766', 'halal_label' => 'certified', 'halal_certificate_no' => 'ID00110012345'])
            ->assertOk()->assertJsonPath('data.whatsapp', '6281299887766');
    }

    public function test_kasir_ditolak_ubah_toko_saldo_dan_anggota(): void
    {
        $this->sebagai($this->kasir)->patchJson($this->url(), ['name' => 'Toko Rina'])->assertForbidden();
        $this->sebagai($this->kasir)->getJson($this->url('/balance'))->assertForbidden();
        $this->sebagai($this->kasir)->getJson($this->url('/balance/transactions'))->assertForbidden();
        $this->sebagai($this->kasir)->getJson($this->url('/members'))->assertForbidden();
        $this->sebagai($this->kasir)->postJson($this->url('/members'), ['phone' => '081311112222', 'name' => 'Budi'])->assertForbidden();
    }

    public function test_saldo_dan_riwayat_transaksi(): void
    {
        $konsumen = User::forceCreate(['phone' => '6285700000001', 'role' => 'consumer']);
        $listing = DB::table('listings')->insertGetId([
            'store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas Pastry Sore', 'price_rupiah' => 18000,
            'qty_total' => 5, 'pickup_date' => '2026-09-18', 'pickup_start' => '2026-09-18 19:00:00', 'pickup_end' => '2026-09-18 21:00:00',
            'status' => 'active', 'ingredients_text' => 'Tepung, mentega',
        ]);
        $order = DB::table('orders')->insertGetId([
            'user_id' => $konsumen->id, 'store_id' => $this->toko, 'code' => 'ORD-1', 'status' => 'completed',
            'subtotal_rupiah' => 18000, 'total_rupiah' => 18000, 'pickup_start' => '2026-09-18 19:00:00', 'pickup_end' => '2026-09-18 21:00:00',
            'placed_at' => now(),
        ]);
        DB::table('order_items')->insert(['order_id' => $order, 'listing_id' => $listing, 'title_snapshot' => 'Tas Pastry Sore', 'unit_price_rupiah' => 18000, 'qty' => 1, 'line_total_rupiah' => 18000]);
        DB::table('pickup_codes')->insert(['order_id' => $order, 'store_id' => $this->toko, 'code' => 'LF7Q2K', 'status' => 'used', 'issued_at' => now(), 'expires_at' => now()->addHour()]);
        DB::table('balance_transactions')->insert([
            ['store_id' => $this->toko, 'order_id' => null, 'type' => 'adjustment', 'amount_rupiah' => 1168000, 'balance_after_rupiah' => 1168000, 'description' => 'Saldo awal', 'created_at' => now()->subDay()],
            ['store_id' => $this->toko, 'order_id' => $order, 'type' => 'sale', 'amount_rupiah' => 18000, 'balance_after_rupiah' => 1186000, 'description' => 'Pesanan ORD-1', 'created_at' => now()],
        ]);

        $this->sebagai($this->pemilik)->getJson($this->url('/balance'))->assertOk()
            ->assertJsonPath('data.available_rupiah', 1186000)
            ->assertJsonPath('data.pending_rupiah', 18000)
            ->assertJsonPath('data.withdrawal.enabled', false);

        $this->sebagai($this->pemilik)->getJson($this->url('/balance/transactions'))->assertOk()
            ->assertJsonPath('data.0.title', 'Tas Pastry Sore')
            ->assertJsonPath('data.0.pickup_code', 'LF7Q2K')
            ->assertJsonPath('data.0.amount_rupiah', 18000)
            ->assertJsonPath('data.1.title', 'Saldo awal')
            ->assertJsonPath('data.1.pickup_code', null);
    }

    public function test_undang_kasir_membuat_akun_mitra(): void
    {
        $data = $this->sebagai($this->pemilik)->postJson($this->url('/members'), ['phone' => '0813 1111 2222', 'name' => 'Budi'])
            ->assertCreated()
            ->assertJsonPath('data.role', 'cashier')
            ->assertJsonPath('data.phone', '6281311112222')
            ->json('data');

        $this->assertStringContainsString('Kopi Kalyan SCBD', $data['invite_message']);
        $this->assertDatabaseHas('users', ['phone' => '6281311112222', 'role' => 'partner']);
        $this->assertDatabaseHas('audit_logs', ['action' => 'store_member.invite', 'subject_id' => $data['id']]);

        $this->sebagai($this->pemilik)->getJson($this->url('/members'))->assertOk()
            ->assertJsonCount(3, 'data')
            ->assertJsonPath('data.0.is_store_owner', true)
            ->assertJsonPath('data.0.name', 'Kalyan Pratama');
    }

    public function test_undang_ditolak_untuk_konsumen_anggota_dan_pemilik(): void
    {
        User::forceCreate(['phone' => '6285700000002', 'role' => 'consumer']);

        $this->sebagai($this->pemilik)->postJson($this->url('/members'), ['phone' => '085700000002', 'name' => 'Sari'])->assertStatus(409);
        $this->sebagai($this->pemilik)->postJson($this->url('/members'), ['phone' => '081322445566', 'name' => 'Rina'])->assertStatus(409);
        $this->sebagai($this->pemilik)->postJson($this->url('/members'), ['phone' => '081299887766', 'name' => 'Kalyan'])->assertStatus(409);
        $this->sebagai($this->pemilik)->postJson($this->url('/members'), ['phone' => '12345', 'name' => 'X'])->assertStatus(422);
    }

    public function test_cabut_kasir_lalu_undang_ulang(): void
    {
        $member = DB::table('store_members')->where('user_id', $this->kasir->id)->value('id');
        $tokenKasir = $this->kasir->createToken('uji')->plainTextToken;

        $this->sebagai($this->pemilik)->deleteJson($this->url("/members/{$member}"))->assertNoContent();
        $this->app['auth']->forgetGuards();
        $this->withToken($tokenKasir)->getJson($this->url())->assertNotFound();

        $this->app['auth']->forgetGuards();
        $this->sebagai($this->pemilik)->postJson($this->url('/members'), ['phone' => '081322445566', 'name' => 'Rina'])
            ->assertCreated()->assertJsonPath('data.id', $member);
    }

    public function test_pemilik_tidak_bisa_dicabut(): void
    {
        $member = DB::table('store_members')->insertGetId(['store_id' => $this->toko, 'user_id' => $this->pemilik->id, 'role' => 'owner']);

        $this->sebagai($this->pemilik)->deleteJson($this->url("/members/{$member}"))->assertStatus(409);
        $this->sebagai($this->pemilik)->deleteJson($this->url('/members/99999'))->assertNotFound();
    }

    private function url(string $akhir = ''): string
    {
        return "/api/partner/stores/{$this->toko}{$akhir}";
    }

    private function sebagai(User $user): self
    {
        $this->app['auth']->forgetGuards();

        return $this->withToken($user->createToken('uji')->plainTextToken);
    }
}
