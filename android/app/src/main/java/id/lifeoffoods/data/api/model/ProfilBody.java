package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * PATCH /me. Server hanya mengubah field yang dikirim, dan Gson tidak menulis field bernilai null,
 * jadi field yang dibiarkan null tidak ikut terkirim.
 */
public class ProfilBody {

    @Nullable
    @SerializedName("name")
    public String name;

    @Nullable
    @SerializedName("email")
    public String email;

    /** Khusus konsumen; akun mitra ditolak 422. */
    @Nullable
    @SerializedName("area_label")
    public String areaLabel;

    /** Sakelar K20, khusus konsumen. */
    @Nullable
    @SerializedName("notify_favorite_store")
    public Boolean notifyFavoriteStore;

    @Nullable
    @SerializedName("notify_pickup_reminder")
    public Boolean notifyPickupReminder;

    @Nullable
    @SerializedName("notify_promo")
    public Boolean notifyPromo;
}
