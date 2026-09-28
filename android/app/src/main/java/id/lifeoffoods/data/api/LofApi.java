package id.lifeoffoods.data.api;

import id.lifeoffoods.data.api.model.AlergenBody;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.AuthResponse;
import id.lifeoffoods.data.api.model.FavoritDto;
import id.lifeoffoods.data.api.model.GoogleLoginBody;
import id.lifeoffoods.data.api.model.ListingDetailDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.data.api.model.OtpRequestBody;
import id.lifeoffoods.data.api.model.OtpRequestResponse;
import id.lifeoffoods.data.api.model.OtpVerifyBody;
import id.lifeoffoods.data.api.model.ProfilBody;
import id.lifeoffoods.data.api.model.Terbungkus;
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

    /** Lencana lonceng K07; daftar lengkap di K17. */
    @GET("notifications")
    Call<NotifikasiResponse> notifikasi(@Query("unread") int belumDibaca);

    /**
     * Token dikirim eksplisit karena sesi lokal langsung dihapus sesudah enqueue, sebelum
     * interceptor sempat membacanya.
     */
    @POST("auth/logout")
    Call<Void> keluar(@Header("Authorization") String bearer);
}
