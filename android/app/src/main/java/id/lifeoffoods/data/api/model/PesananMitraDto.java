package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Pesanan dari sisi mitra: baris GET /partner/stores/{store}/orders (M11) dan respons 200 POST
 * /pickup-codes/redeem (M12). Kode pickup sengaja tidak ada; kasir mengetik kode dari HP pembeli.
 */
public class PesananMitraDto {

    @SerializedName("id")
    public long id;

    /** Nomor pesanan (LOF-7Q2K9A), bukan kode pickup. */
    @SerializedName("code")
    public String code;

    @SerializedName("status")
    public String status;

    /** Nama depan pembeli saja (PRD-10 kriteria 2). */
    @SerializedName("buyer_name")
    public String buyerName;

    @SerializedName("items")
    public List<Butir> items;

    @SerializedName("item_count")
    public int itemCount;

    @SerializedName("total_rupiah")
    public long totalRupiah;

    /** {@code cash} atau {@code qris_static}. */
    @SerializedName("payment_method")
    public String paymentMethod;

    @SerializedName("payment_status")
    public String paymentStatus;

    @Nullable
    @SerializedName("note")
    public String note;

    /** Alergi pembeli saat memesan. */
    @Nullable
    @SerializedName("allergen_snapshot")
    public List<Alergi> allergenSnapshot;

    @SerializedName("pickup_start")
    public String pickupStart;

    @SerializedName("pickup_end")
    public String pickupEnd;

    @SerializedName("placed_at")
    public String placedAt;

    @Nullable
    @SerializedName("completed_at")
    public String completedAt;

    /** Hanya ada di riwayat (M21); pesanan yang menunggu tidak pernah membawa kodenya. */
    @SerializedName("pickup_code")
    public String pickupCode;

    public static class Butir {
        @SerializedName("title")
        public String title;

        @SerializedName("qty")
        public int qty;

        @SerializedName("line_total_rupiah")
        public long lineTotalRupiah;
    }

    public static class Alergi {
        @SerializedName("code")
        public String code;

        @SerializedName("name")
        public String name;

        /** {@code avoid} atau {@code severe}. */
        @SerializedName("severity")
        public String severity;
    }
}
