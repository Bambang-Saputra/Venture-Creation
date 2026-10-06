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

    /** Dipegang + terjual. Dipakai label "N dipesan hari ini" di Populer hari ini. */
    @SerializedName("qty_ordered")
    public int qtyOrdered;

    /**
     * true kalau jualan ini ditaruh paling atas karena tokonya membeli paket prioritas pencarian.
     * Kartunya wajib memakai label "Iklan".
     */
    @SerializedName("is_sponsored")
    public boolean isSponsored;

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

    /** certified, self_claim, atau not_stated. */
    @Nullable
    @SerializedName("halal_label")
    public String halalLabel;

    @Nullable
    @SerializedName("allergens")
    public List<Alergen> allergens;

    @SerializedName("store")
    public Toko store;

    /** Ringkas di GET /listings; alamat, koordinat, dan jam hari ini hanya ada di detail. */
    public static class Toko {
        @SerializedName("id")
        public long id;

        @SerializedName("name")
        public String name;

        @SerializedName("category")
        public String category;

        @Nullable
        @SerializedName("address")
        public String address;

        @Nullable
        @SerializedName("latitude")
        public Double latitude;

        @Nullable
        @SerializedName("longitude")
        public Double longitude;

        @Nullable
        @SerializedName("hours_today")
        public JamHariIni hoursToday;

        /** Rata-rata bintang, satu angka di belakang koma. Null kalau belum ada ulasan. */
        @Nullable
        @SerializedName("rating_average")
        public Double ratingAverage;

        @SerializedName("rating_count")
        public int ratingCount;
    }

    public static class JamHariIni {
        /** "07:00:00". */
        @Nullable
        @SerializedName("open_time")
        public String openTime;

        @Nullable
        @SerializedName("close_time")
        public String closeTime;

        /** Server mengirim 0/1. */
        @SerializedName("is_closed")
        public int isClosed;
    }

    public static class Alergen {
        public static final String MUNGKIN = "may_contain";

        @SerializedName("code")
        public String code;

        @SerializedName("name")
        public String name;

        /** contains atau may_contain. */
        @SerializedName("presence")
        public String presence;
    }
}
