package id.lifeoffoods.data.api;

import id.lifeoffoods.data.api.model.AuthResponse;
import id.lifeoffoods.data.api.model.GoogleLoginBody;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.OtpRequestBody;
import id.lifeoffoods.data.api.model.OtpRequestResponse;
import id.lifeoffoods.data.api.model.OtpVerifyBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

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

    /**
     * Token dikirim eksplisit karena sesi lokal langsung dihapus sesudah enqueue, sebelum
     * interceptor sempat membacanya.
     */
    @POST("auth/logout")
    Call<Void> keluar(@Header("Authorization") String bearer);
}
