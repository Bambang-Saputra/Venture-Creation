package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Jawaban POST /orders/preview (K12, K13) dan GET /orders/{id} (K14). Keduanya berbagi toko, item,
 * jam ambil, dan rincian biaya; field pesanan (kode, status) hanya ada di detail.
 */
public class PesananDto {

    public static final String STATUS_MENUNGGU = "pending_pickup";
    public static final String STATUS_SELESAI = "completed";
    public static final String STATUS_BATAL = "cancelled";
    public static final String STATUS_TIDAK_DIAMBIL = "no_show";

    @SerializedName("id")
    public long id;

    @Nullable
    @SerializedName("code")
    public String code;

    @Nullable
    @SerializedName("status")
    public String status;

    /** Hanya terisi selama kodenya masih bisa ditukar. */
    @Nullable
    @SerializedName("pickup_code")
    public String pickupCode;

    @SerializedName("store")
    public Toko store;

    @Nullable
    @SerializedName("items")
    public List<Item> items;

    @SerializedName("pickup_start")
    public String pickupStart;

    @SerializedName("pickup_end")
    public String pickupEnd;

    @SerializedName("item_count")
    public int itemCount;

    @SerializedName("subtotal_rupiah")
    public long subtotalRupiah;

    @SerializedName("service_fee_rupiah")
    public long serviceFeeRupiah;

    @SerializedName("discount_rupiah")
    public long discountRupiah;

    @SerializedName("total_rupiah")
    public long totalRupiah;

    @Nullable
    @SerializedName("payment_method")
    public String paymentMethod;

    /** unpaid atau paid; bayar di tempat, jadi lunas saat kode ditukar (ADR-0004). */
    @Nullable
    @SerializedName("payment_status")
    public String paymentStatus;

    /** Preview saja: alergi di profil pembeli yang ada di jualan ini. Tidak memblokir. */
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
    }

    public static class Item {
        @SerializedName("listing_id")
        public long listingId;

        @SerializedName("title")
        public String title;

        @SerializedName("qty")
        public int qty;

        @SerializedName("unit_price_rupiah")
        public long unitPriceRupiah;

        /** Harga normal untuk dicoret; hanya ada di pratinjau. */
        @SerializedName("original_value_rupiah")
        public Long originalValueRupiah;

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

        @SerializedName("presence")
        public String presence;
    }
}
