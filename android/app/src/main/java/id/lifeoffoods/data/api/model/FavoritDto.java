package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Satu toko di GET /favorites (K16, tab Mitra). Status hati di K10/K11 hanya memakai id. */
public class FavoritDto {

    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    /** bakery, cafe, resto, catering, atau grocery. */
    @Nullable
    @SerializedName("category")
    public String category;

    @Nullable
    @SerializedName("photo_url")
    public String photoUrl;

    /** Null kalau lat/lng tidak dikirim atau toko belum punya koordinat. */
    @Nullable
    @SerializedName("distance_km")
    public Double distanceKm;

    /** "21:00", atau null kalau tutup hari ini. */
    @Nullable
    @SerializedName("closes_at")
    public String closesAt;

    @SerializedName("is_temporarily_closed")
    public boolean isTemporarilyClosed;

    /** Sisa tas kejutan yang masih bisa dipesan hari ini. */
    @SerializedName("available_bags")
    public int availableBags;

    @SerializedName("has_menu_available")
    public boolean hasMenuAvailable;

    /** "19:00": jam terbit yang paling sering 4 minggu terakhir, atau null. */
    @Nullable
    @SerializedName("usual_publish_time")
    public String usualPublishTime;

    /** Body POST /favorites. */
    public static class Body {
        @SerializedName("store_id")
        public final long storeId;

        public Body(long storeId) {
            this.storeId = storeId;
        }
    }
}
