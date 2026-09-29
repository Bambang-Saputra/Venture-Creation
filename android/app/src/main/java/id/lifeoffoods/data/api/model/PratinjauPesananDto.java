package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** POST /orders/preview (K12, K13). Tidak menyimpan apa pun di server. */
public class PratinjauPesananDto {

    @SerializedName("store")
    public Toko store;

    @SerializedName("items")
    public List<Baris> items;

    @SerializedName("pickup_start")
    public String pickupStart;

    @SerializedName("pickup_end")
    public String pickupEnd;

    @SerializedName("subtotal_rupiah")
    public long subtotalRupiah;

    @SerializedName("service_fee_rupiah")
    public long serviceFeeRupiah;

    @SerializedName("discount_rupiah")
    public long discountRupiah;

    @SerializedName("total_rupiah")
    public long totalRupiah;

    /** Tidak memblokir pesanan, tapi wajib tampil sebelum tombol "Buat pesanan". */
    @Nullable
    @SerializedName("allergen_warnings")
    public List<PeringatanAlergen> allergenWarnings;

    public static class Toko {
        @SerializedName("id")
        public long id;

        @SerializedName("name")
        public String name;

        @Nullable
        @SerializedName("address")
        public String address;

        /** Hanya ada di GET /orders/{id}. */
        @Nullable
        @SerializedName("latitude")
        public Double latitude;

        @Nullable
        @SerializedName("longitude")
        public Double longitude;
    }

    public static class Baris {
        @SerializedName("listing_id")
        public long listingId;

        @SerializedName("title")
        public String title;

        @SerializedName("qty")
        public int qty;

        @SerializedName("unit_price_rupiah")
        public long unitPriceRupiah;

        @SerializedName("line_total_rupiah")
        public long lineTotalRupiah;
    }

    public static class PeringatanAlergen {
        @SerializedName("listing_id")
        public long listingId;

        @SerializedName("code")
        public String code;

        @SerializedName("name")
        public String name;

        /** {@code contains} atau {@code may_contain}. */
        @SerializedName("presence")
        public String presence;

        /** {@code avoid} atau {@code severe}, dari profil alergi pembeli. */
        @SerializedName("severity")
        public String severity;
    }
}
