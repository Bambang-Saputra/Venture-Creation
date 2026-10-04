package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Model kecil untuk akun pembeli K18-K20. */
public final class AkunDto {

    private AkunDto() {}

    /** GET /me/impact: header K18. */
    public static class Dampak {
        @SerializedName("portions_rescued")
        public int portionsRescued;

        @SerializedName("saved_rupiah")
        public long savedRupiah;

        @SerializedName("orders_completed")
        public int ordersCompleted;

        @Nullable
        @SerializedName("member_since")
        public String memberSince;
    }

    /** Jawaban POST /me/photo. */
    public static class Foto {
        @Nullable
        @SerializedName("photo_url")
        public String photoUrl;
    }

    /** Body DELETE /me (K20 Hapus akun). */
    public static class HapusAkun {
        @SerializedName("confirm")
        public final boolean confirm = true;
    }
}
