package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Body dan isi jawaban POST /orders/{id}/review (K15, kontrak API bagian 6). */
public class UlasanDto {

    /** 1 sampai 5. */
    @SerializedName("rating")
    public int rating;

    /** Opsional, maksimal 500 karakter. */
    @Nullable
    @SerializedName("comment")
    public String comment;

    public UlasanDto(int rating, @Nullable String comment) {
        this.rating = rating;
        this.comment = comment;
    }
}
