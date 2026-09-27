package id.lifeoffoods.data.api;

import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback Retrofit yang memisahkan sukses dan gagal, supaya setiap ViewModel tidak mengulang
 * pemeriksaan isSuccessful dan pembacaan errorBody. Dipanggil di main thread oleh Retrofit.
 *
 * <pre>
 * api.saya().enqueue(new ApiCallback&lt;&gt;() {
 *     public void sukses(MeResponse data) { ... }
 *     public void gagal(ApiError galat) { ... }
 * });
 * </pre>
 */
public abstract class ApiCallback<T> implements Callback<T> {

    public abstract void sukses(T data);

    public abstract void gagal(ApiError galat);

    @Override
    public final void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        if (response.isSuccessful()) {
            sukses(response.body());
            return;
        }
        String body = null;
        try (ResponseBody err = response.errorBody()) {
            if (err != null) {
                body = err.string();
            }
        } catch (IOException e) {
            // Body galat tidak terbaca; ApiError memakai pesan umum.
        }
        gagal(ApiError.dariRespons(response.code(), body));
    }

    @Override
    public final void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        if (!call.isCanceled()) {
            gagal(ApiError.jaringan());
        }
    }
}
