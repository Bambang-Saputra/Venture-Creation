package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Satu item GET /listings (kontrak API bagian 4). Dipakai K07, K08, dan K09. */
public class ListingDto {

    public static final String TIPE_TAS = "surprise_bag";
    public static final String TIPE_MENU = "menu_item";

    @SerializedName("id")
    public long id;

    /** surprise_bag (buka K10) atau menu_item (buka K11). */
    @SerializedName("type")
    public String type;

    @SerializedName("title")
    public String title;

    @Nullable
    @SerializedName("photo_url")
    public String photoUrl;

    @SerializedName("price_rupiah")
    public long priceRupiah;

    @Nullable
    @SerializedName("original_value_rupiah")
    public Long originalValueRupiah;

    @SerializedName("qty_remaining")
    public int qtyRemaining;

    /** ISO 8601 dengan offset +07:00. */
    @SerializedName("pickup_start")
    public String pickupStart;

    @SerializedName("pickup_end")
    public String pickupEnd;

    @SerializedName("minutes_until_end")
    public int minutesUntilEnd;

    @Nullable
    @SerializedName("distance_km")
    public Double distanceKm;

    @Nullable
    @SerializedName("allergens")
    public List<Alergen> allergens;

    @SerializedName("store")
    public Toko store;

    public static class Toko {
        @SerializedName("id")
        public long id;

        @SerializedName("name")
        public String name;

        @SerializedName("category")
        public String category;
    }

    public static class Alergen {
        @SerializedName("code")
        public String code;

        @SerializedName("name")
        public String name;

        /** contains atau may_contain. */
        @SerializedName("presence")
        public String presence;
    }
}
