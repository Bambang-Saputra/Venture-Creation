package id.lifeoffoods.data.api;

import id.lifeoffoods.data.SesiPengguna;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Membuat Retrofit sesuai aturan umum docs/api/kontrak-api.md bagian 1. */
public final class ApiClient {

    private ApiClient() {}

    public static LofApi buat(String baseUrl, SesiPengguna sesi, boolean debug) {
        OkHttpClient.Builder http =
                new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .addInterceptor(
                                chain -> {
                                    Request.Builder req =
                                            chain.request()
                                                    .newBuilder()
                                                    .header("Accept", "application/json")
                                                    // Tanpa ini ngrok membalas halaman peringatan
                                                    // HTML.
                                                    .header("ngrok-skip-browser-warning", "1");
                                    String token = sesi.token();
                                    if (token != null
                                            && chain.request().header("Authorization") == null) {
                                        req.header("Authorization", "Bearer " + token);
                                    }
                                    return chain.proceed(req.build());
                                });

        if (debug) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            // BASIC: method, URL, status. BODY akan ikut mencetak token dan kode OTP ke logcat.
            log.setLevel(HttpLoggingInterceptor.Level.BASIC);
            http.addInterceptor(log);
        }

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(http.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LofApi.class);
    }
}
