package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respons yang dibungkus {@code data} (semua endpoint kecuali auth dan /me, kontrak API bagian 1).
 * Contoh: {@code Call<Terbungkus<List<AlergenDto>>>}.
 */
public class Terbungkus<T> {

    @SerializedName("data")
    public T data;
}
