<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

/**
 * Unggah foto (F-20): profil, toko, template tas, jualan.
 */
class FotoTest extends TestCase
{
    use RefreshDatabase;

    private User $pemilik;

    private User $kasir;

    private int $toko;

    protected function setUp(): void
    {
        parent::setUp();
        Storage::fake('public');

        $this->pemilik = User::forceCreate(['phone' => '6281299887766', 'name' => 'Kalyan Pratama', 'role' => 'partner'])->refresh();
        $this->kasir = User::forceCreate(['phone' => '6281322445566', 'name' => 'Rina', 'role' => 'partner'])->refresh();
        $this->toko = DB::table('stores')->insertGetId([
            'owner_user_id' => $this->pemilik->id, 'name' => 'Kopi Kalyan SCBD', 'slug' => 'kopi-kalyan', 'category' => 'cafe',
            'address' => 'Jl. Jend. Sudirman Kav 52',
        ]);
        DB::table('store_members')->insert(['store_id' => $this->toko, 'user_id' => $this->kasir->id, 'role' => 'cashier', 'invited_at' => now()]);
    }

    public function test_foto_profil_disimpan_ulang_sebagai_jpeg_kecil_lalu_bisa_dihapus(): void
    {
        $user = User::forceCreate(['phone' => '6281200000001', 'name' => 'Dinda', 'role' => 'consumer'])->refresh();

        $url = $this->sebagai($user)->post('/api/me/photo', ['photo' => UploadedFile::fake()->image('wajah.png', 3000, 2000)], ['Accept' => 'application/json'])
            ->assertOk()->json('photo_url');

        $path = DB::table('users')->where('id', $user->id)->value('photo_path');
        $this->assertStringStartsWith("users/{$user->id}/", $path);
        $this->assertStringEndsWith('.jpg', $path);
        $this->assertStringEndsWith($path, $url);
        Storage::disk('public')->assertExists($path);

        // Sisi terpanjang dikecilkan ke 1600 px, dan hasilnya benar-benar JPEG.
        [$lebar, $tinggi, $tipe] = getimagesizefromstring(Storage::disk('public')->get($path));
        $this->assertSame([1600, 1067, IMAGETYPE_JPEG], [$lebar, $tinggi, $tipe]);

        $this->sebagai($user)->getJson('/api/me')->assertJsonPath('user.photo_url', $url);

        $this->sebagai($user)->deleteJson('/api/me/photo')->assertOk()->assertJsonPath('photo_url', null);
        Storage::disk('public')->assertMissing($path);
    }

    public function test_ganti_foto_menghapus_berkas_lama(): void
    {
        $this->sebagai($this->pemilik)->post($this->url('/photo'), ['photo' => UploadedFile::fake()->image('a.jpg', 800, 600)], ['Accept' => 'application/json'])->assertOk();
        $lama = DB::table('stores')->where('id', $this->toko)->value('photo_path');

        $this->sebagai($this->pemilik)->post($this->url('/photo'), ['photo' => UploadedFile::fake()->image('b.jpg', 800, 600)], ['Accept' => 'application/json'])->assertOk();
        $baru = DB::table('stores')->where('id', $this->toko)->value('photo_path');

        $this->assertNotSame($lama, $baru);
        Storage::disk('public')->assertMissing($lama);
        Storage::disk('public')->assertExists($baru);
        $this->sebagai($this->pemilik)->getJson($this->url())->assertJsonPath('data.photo_url', asset('storage/'.$baru));
    }

    public function test_foto_jualan_tidak_menghapus_foto_template_yang_dipakai_bersama(): void
    {
        Storage::disk('public')->put('templates/9/lama.jpg', 'x');
        $listing = DB::table('listings')->insertGetId([
            'store_id' => $this->toko, 'type' => 'surprise_bag', 'title' => 'Tas Pastry Sore', 'price_rupiah' => 18000,
            'qty_total' => 5, 'pickup_date' => '2026-09-18', 'pickup_start' => '2026-09-18 19:00:00', 'pickup_end' => '2026-09-18 21:00:00',
            'status' => 'draft', 'photo_path' => 'templates/9/lama.jpg',
        ]);

        $this->sebagai($this->pemilik)->post($this->url("/listings/{$listing}/photo"), ['photo' => UploadedFile::fake()->image('tas.jpg', 800, 800)], ['Accept' => 'application/json'])
            ->assertOk();

        $this->assertStringStartsWith("listings/{$listing}/", DB::table('listings')->where('id', $listing)->value('photo_path'));
        Storage::disk('public')->assertExists('templates/9/lama.jpg');
    }

    public function test_jualan_tanpa_foto_memakai_foto_toko_di_sisi_pembeli(): void
    {
        $url = $this->sebagai($this->pemilik)->post($this->url('/photo'), ['photo' => UploadedFile::fake()->image('toko.jpg', 800, 600)], ['Accept' => 'application/json'])
            ->assertOk()->json('photo_url');
        $buat = fn (string $judul, ?string $foto) => DB::table('listings')->insertGetId([
            'store_id' => $this->toko, 'type' => 'menu_item', 'title' => $judul, 'price_rupiah' => 9000,
            'qty_total' => 3, 'pickup_date' => now()->toDateString(), 'pickup_start' => now()->subHour(), 'pickup_end' => now()->addHours(2),
            'status' => 'active', 'photo_path' => $foto, 'ingredients_text' => 'Tepung, mentega',
        ]);
        $tanpa = $buat('Croissant', null);
        Storage::disk('public')->put('listings/99/sendiri.jpg', 'x');
        $sendiri = $buat('Danish', 'listings/99/sendiri.jpg');

        $this->getJson("/api/listings/{$tanpa}")->assertOk()->assertJsonPath('data.photo_url', $url);
        $this->assertStringEndsWith('listings/99/sendiri.jpg', $this->getJson("/api/listings/{$sendiri}")->assertOk()->json('data.photo_url'));
    }

    public function test_foto_template_dipakai_tas_baru(): void
    {
        $template = DB::table('surprise_bag_templates')->insertGetId([
            'store_id' => $this->toko, 'name' => 'Tas Pastry Sore', 'price_rupiah' => 18000, 'original_value_rupiah' => 54000,
            'default_qty' => 5, 'pickup_start_time' => '19:00', 'pickup_end_time' => '21:00', 'ingredients_text' => 'Tepung, mentega',
        ]);

        $url = $this->sebagai($this->pemilik)->post($this->url("/templates/{$template}/photo"), ['photo' => UploadedFile::fake()->image('tas.jpg', 800, 800)], ['Accept' => 'application/json'])
            ->assertOk()->json('photo_url');

        $this->sebagai($this->pemilik)->getJson($this->url('/templates'))->assertOk()->assertJsonPath('data.0.photo_url', $url);
    }

    public function test_bukan_gambar_terlalu_kecil_dan_terlalu_besar_ditolak(): void
    {
        $kirim = fn (UploadedFile $f) => $this->sebagai($this->pemilik)->post($this->url('/photo'), ['photo' => $f], ['Accept' => 'application/json']);

        $kirim(UploadedFile::fake()->create('virus.jpg', 10, 'application/x-msdownload'))->assertStatus(422)->assertJsonValidationErrors('photo');
        $kirim(UploadedFile::fake()->image('kecil.jpg', 100, 100))->assertStatus(422)->assertJsonValidationErrors('photo');
        $kirim(UploadedFile::fake()->image('raksasa.jpg', 5000, 3000))->assertStatus(422)->assertJsonValidationErrors('photo');
        $kirim(UploadedFile::fake()->image('berat.jpg', 800, 800)->size(6000))->assertStatus(422)->assertJsonValidationErrors('photo');

        $this->assertNull(DB::table('stores')->where('id', $this->toko)->value('photo_path'));
    }

    public function test_hanya_pemilik_dan_hanya_toko_sendiri(): void
    {
        $foto = fn () => ['photo' => UploadedFile::fake()->image('a.jpg', 800, 600)];

        $this->sebagai($this->kasir)->post($this->url('/photo'), $foto(), ['Accept' => 'application/json'])->assertForbidden();

        $lain = User::forceCreate(['phone' => '6281311112222', 'name' => 'Lain', 'role' => 'partner'])->refresh();
        $this->sebagai($lain)->post($this->url('/photo'), $foto(), ['Accept' => 'application/json'])->assertNotFound();

        $this->sebagai($this->pemilik)->post($this->url('/listings/999999/photo'), $foto(), ['Accept' => 'application/json'])->assertNotFound();
        $this->sebagai($this->pemilik)->post($this->url('/templates/999999/photo'), $foto(), ['Accept' => 'application/json'])->assertNotFound();
    }

    public function test_hapus_akun_menghapus_foto_profil(): void
    {
        $user = User::forceCreate(['phone' => '6281200000001', 'name' => 'Dinda', 'role' => 'consumer'])->refresh();
        $this->sebagai($user)->post('/api/me/photo', ['photo' => UploadedFile::fake()->image('wajah.jpg', 800, 800)], ['Accept' => 'application/json'])->assertOk();
        $path = DB::table('users')->where('id', $user->id)->value('photo_path');

        $this->sebagai($user)->deleteJson('/api/me', ['confirm' => true])->assertSuccessful();

        Storage::disk('public')->assertMissing($path);
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
