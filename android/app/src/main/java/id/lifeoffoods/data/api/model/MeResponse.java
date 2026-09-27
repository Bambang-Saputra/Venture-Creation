package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Respons GET /me (tidak dibungkus data). */
public class MeResponse {

    @SerializedName("user")
    public UserDto user;

    /** false: nama belum diisi, buka K04. */
    @SerializedName("is_profile_complete")
    public boolean isProfileComplete;

    /** null untuk akun mitra. */
    @Nullable
    @SerializedName("consumer_profile")
    public ConsumerProfile consumerProfile;

    @SerializedName("allergens")
    public List<Alergen> allergens;

    public static class ConsumerProfile {
        @Nullable
        @SerializedName("area_label")
        public String areaLabel;

        @Nullable
        @SerializedName("latitude")
        public Double latitude;

        @Nullable
        @SerializedName("longitude")
        public Double longitude;

        @SerializedName("notify_favorite_store")
        public boolean notifyFavoriteStore;

        @SerializedName("notify_pickup_reminder")
        public boolean notifyPickupReminder;

        @SerializedName("notify_promo")
        public boolean notifyPromo;
    }

    public static class Alergen {
        @SerializedName("code")
        public String code;

        @SerializedName("name")
        public String name;

        /** allergen atau diet. */
        @SerializedName("type")
        public String type;

        /** avoid atau severe. */
        @SerializedName("severity")
        public String severity;
    }
}
