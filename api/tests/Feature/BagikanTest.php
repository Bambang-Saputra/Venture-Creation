<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

/**
 * Halaman tautan bagikan /j/{id}.
 */
class BagikanTest extends TestCase
{
    use RefreshDatabase;

    public function test_halaman_bagikan_punya_pratinjau_dan_tombol_aplikasi(): void
    {
        $this->travelTo(now()->setTime(17, 0));
        $pemilik = User::forceCreate(['phone' => '6281200000001', 'role' => 'partner']);
        $toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $pemilik->id, 'name' => 'Roti Sari Bakery', 'slug' => 'roti-sari',
            'category' => 'bakery', 'address' => 'Jl. Tebet Raya No. 12',
        ]);
        $id = DB::table('listings')->insertGetId([
            'store_id' => $toko, 'type' => 'surprise_bag', 'title' => 'Tas Roti Sore', 'price_rupiah' => 21000,
            'original_value_rupiah' => 60000, 'qty_total' => 5, 'pickup_date' => now()->toDateString(),
            'pickup_start' => now()->setTime(18, 0), 'pickup_end' => now()->setTime(21, 0),
            'status' => 'active', 'ingredients_text' => 'Tepung, gula',
        ]);

        $this->get("/j/{$id}")
            ->assertOk()
            ->assertSee('<meta property="og:title" content="Tas Roti Sore, Rp21.000 (hemat 65%)">', false)
            ->assertSee('Ambil hari ini jam 18.00 sampai 21.00')
            ->assertSee('package=id.lifeoffoods', false);
    }

    public function test_jualan_draft_atau_tidak_ada_404(): void
    {
        $this->get('/j/999999')->assertNotFound();
    }
}
