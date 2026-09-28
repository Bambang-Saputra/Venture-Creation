package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Satu toko di GET /favorites. Baru {@code id} yang dipakai (status hati di K10/K11); kolom lain
 * ditambahkan saat K16 dikerjakan.
 */
public class FavoritDto {

    @SerializedName("id")
    public long id;

    /** Body POST /favorites. */
    public static class Body {
        @SerializedName("store_id")
        public final long storeId;

        public Body(long storeId) {
            this.storeId = storeId;
        }
    }
}
