package id.lifeoffoods.data.api;

import id.lifeoffoods.data.api.model.AlergenBody;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.AuthResponse;
import id.lifeoffoods.data.api.model.CatatSisaBody;
import id.lifeoffoods.data.api.model.CatatanSisaDto;
import id.lifeoffoods.data.api.model.FavoritDto;
import id.lifeoffoods.data.api.model.GoogleLoginBody;
import id.lifeoffoods.data.api.model.HalamanListing;
import id.lifeoffoods.data.api.model.HalamanPesanan;
import id.lifeoffoods.data.api.model.HalamanPesananMitra;
import id.lifeoffoods.data.api.model.JualanBody;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.LaporanMingguanDto;
import id.lifeoffoods.data.api.model.ListingDetailDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.data.api.model.OtpRequestBody;
import id.lifeoffoods.data.api.model.OtpRequestResponse;
import id.lifeoffoods.data.api.model.OtpVerifyBody;
import id.lifeoffoods.data.api.model.PesananBody;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import id.lifeoffoods.data.api.model.PratinjauPesananDto;
import id.lifeoffoods.data.api.model.ProdukDto;
import id.lifeoffoods.data.api.model.ProfilBody;
import id.lifeoffoods.data.api.model.RingkasanTokoDto;
import id.lifeoffoods.data.api.model.TemplateTasDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.data.api.model.TokoMitraDto;
import id.lifeoffoods.data.api.model.TukarKodeBody;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Endpoint REST Life of Foods. Path relatif terhadap API_BASE_URL (sudah berakhiran /api/).
 *
 * <p>Tambahkan endpoint di sini mengikuti docs/api/kontrak-api.md saat layarnya dikerjakan.
 */
public interface LofApi {

    /** K02, M01. */
    @POST("auth/otp/request")
    Call<OtpRequestResponse> mintaOtp(@Body OtpRequestBody body);

    /** K03, M02. */
    @POST("auth/otp/verify")
    Call<AuthResponse> verifikasiOtp(@Body OtpVerifyBody body);

    /** Tombol "Lanjut dengan akun Google" di K02 dan M01 (ADR-0006). */
    @POST("auth/google")
    Call<AuthResponse> masukGoogle(@Body GoogleLoginBody body);

    /** Pemeriksaan sesi saat aplikasi dibuka, K18. */
    @GET("me")
    Call<MeResponse> saya();

    /** K04 Lengkapi profil; nanti juga K19 dan K20. Respons sama dengan GET /me. */
    @PATCH("me")
    Call<MeResponse> ubahProfil(@Body ProfilBody body);

    /** Chip K05 dan filter K09. Tanpa token. */
    @GET("allergens")
    Call<Terbungkus<List<AlergenDto>>> daftarAlergen();

    /** K05: mengganti seluruh pilihan alergi dan pola makan. */
    @PUT("me/allergens")
    Call<Terbungkus<List<MeResponse.Alergen>>> simpanAlergen(@Body AlergenBody body);

    /**
     * K07, K08, K09. Tanpa token. Kunci query mengikuti kontrak API (lat, lng, category,
     * ends_within_minutes, per_page, ...); nilai dikirim sebagai teks berformat Locale.US.
     */
    @GET("listings")
    Call<Terbungkus<List<ListingDto>>> daftarListing(@QueryMap Map<String, String> query);

    /**
     * K07 dan K09 dengan filter (PRD-04). Kunci array (type[], exclude_allergens[]) tidak bisa
     * lewat QueryMap karena satu kunci punya banyak nilai; list null tidak dikirim. Jawabannya
     * halaman Laravel, jadi {@code total} bisa dipakai untuk "Tampilkan 12 jualan".
     */
    @GET("listings")
    Call<HalamanListing> daftarListingTersaring(
            @QueryMap Map<String, String> query,
            @Query("type[]") List<String> tipe,
            @Query("exclude_allergens[]") List<String> alergen);

    /** K10, K11. Tanpa token. Habis atau lewat tetap bisa dibuka dengan is_available = false. */
    @GET("listings/{id}")
    Call<Terbungkus<ListingDetailDto>> detailListing(@Path("id") long id);

    /** Status hati di K10/K11; daftar lengkap di K16. */
    @GET("favorites")
    Call<Terbungkus<List<FavoritDto>>> favorit();

    @POST("favorites")
    Call<Void> tambahFavorit(@Body FavoritDto.Body body);

    @DELETE("favorites/{store}")
    Call<Void> hapusFavorit(@Path("store") long storeId);

    /** K12, K13 saat dibuka: harga, jam ambil, dan peringatan alergen. Tidak menyimpan apa pun. */
    @POST("orders/preview")
    Call<Terbungkus<PratinjauPesananDto>> pratinjauPesanan(@Body PesananBody body);

    /** Tombol "Buat pesanan" di K12/K13. 201 berisi detail pesanan untuk K14. */
    @POST("orders")
    Call<Terbungkus<PesananDto>> buatPesanan(@Body PesananBody body);

    /** K14 kode pickup. */
    @GET("orders/{id}")
    Call<Terbungkus<PesananDto>> detailPesanan(@Path("id") long id);

    /** K15 Pesanan saya. {@code status} = {@code active} atau {@code history}; 20 per halaman. */
    @GET("orders")
    Call<HalamanPesanan> daftarPesanan(@Query("status") String status, @Query("page") int halaman);

    /**
     * "Batalkan pesanan" di K14. 409 kalau statusnya bukan pending_pickup. Body alasan opsional.
     */
    @POST("orders/{id}/cancel")
    Call<Terbungkus<PesananDto>> batalkanPesanan(@Path("id") long id);

    /** Lencana lonceng K07; daftar lengkap di K17. */
    @GET("notifications")
    Call<NotifikasiResponse> notifikasi(@Query("unread") int belumDibaca);

    /** Toko milik akun mitra (pemilik atau kasir); dipakai untuk mendapatkan store_id. */
    @GET("partner/stores")
    Call<Terbungkus<List<TokoMitraDto>>> tokoSaya();

    /**
     * M11 Pesanan masuk dan M21 Riwayat. {@code status} = {@code pending} (urut jam ambil) atau
     * {@code history}; {@code date} (yyyy-MM-dd, opsional) menyaring tanggal ambil.
     */
    @GET("partner/stores/{store}/orders")
    Call<HalamanPesananMitra> pesananMitra(
            @Path("store") long storeId,
            @Query("status") String status,
            @Query("date") String tanggal,
            @Query("page") int halaman);

    /** M12 Cocokkan kode. 404 kode tidak ada di toko ini, 409 sudah dipakai, batal, atau lewat. */
    @POST("pickup-codes/redeem")
    Call<Terbungkus<PesananMitraDto>> tukarKode(@Body TukarKodeBody body);

    /** M09: cetakan tas kejutan. Hanya pemilik (kasir 403). */
    @GET("partner/stores/{store}/templates")
    Call<Terbungkus<List<TemplateTasDto>>> templateTas(@Path("store") long storeId);

    /** M16: produk toko untuk menu satuan. Hanya pemilik. */
    @GET("partner/stores/{store}/products")
    Call<Terbungkus<List<ProdukDto>>> produkToko(@Path("store") long storeId);

    /** M10, M17: jualan hari ini. {@code type} = surprise_bag atau menu_item. */
    @GET("partner/stores/{store}/listings")
    Call<Terbungkus<List<JualanMitraDto>>> jualanMitra(
            @Path("store") long storeId, @Query("type") String tipe);

    /** M09, M16. 201 berisi jualan yang dibuat; 422 kandungan kosong atau jam ambil lewat. */
    @POST("partner/stores/{store}/listings")
    Call<Terbungkus<List<JualanMitraDto>>> pasangJualan(
            @Path("store") long storeId, @Body JualanBody body);

    /** Saklar M10/M17: dari draft atau paused. 409 status lain, 422 jam ambil lewat. */
    @POST("partner/stores/{store}/listings/{listing}/publish")
    Call<Terbungkus<JualanMitraDto>> terbitkanJualan(
            @Path("store") long storeId, @Path("listing") long id);

    /** Saklar M10/M17: dari active. Pesanan yang sudah masuk tetap berlaku. */
    @POST("partner/stores/{store}/listings/{listing}/pause")
    Call<Terbungkus<JualanMitraDto>> jedaJualan(
            @Path("store") long storeId, @Path("listing") long id);

    /** M06, M19. {@code date} yyyy-MM-dd, bawaan hari ini. Kasir boleh. */
    @GET("partner/stores/{store}/waste-logs")
    Call<Terbungkus<CatatanSisaDto>> catatanSisa(
            @Path("store") long storeId, @Query("date") String tanggal);

    /** M06, M19. 409 terkunci (lewat akhir hari berikutnya), 422 tanggal mendatang. */
    @POST("partner/stores/{store}/waste-logs")
    Call<Terbungkus<CatatanSisaDto>> simpanSisa(
            @Path("store") long storeId, @Body CatatSisaBody body);

    /** M05 (dan pil "Tutup 21.00" di M06). Kasir boleh. */
    @GET("partner/stores/{store}/summary")
    Call<Terbungkus<RingkasanTokoDto>> ringkasanToko(@Path("store") long storeId);

    /** M07. {@code weekStart} Senin yyyy-MM-dd. Hanya pemilik (kasir 403). */
    @GET("partner/stores/{store}/reports/weekly")
    Call<Terbungkus<LaporanMingguanDto>> laporanMingguan(
            @Path("store") long storeId, @Query("week_start") String weekStart);

    /**
     * Token dikirim eksplisit karena sesi lokal langsung dihapus sesudah enqueue, sebelum
     * interceptor sempat membacanya.
     */
    @POST("auth/logout")
    Call<Void> keluar(@Header("Authorization") String bearer);
}
