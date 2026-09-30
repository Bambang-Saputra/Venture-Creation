package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** GET /orders/{id}, juga respons 201 POST /orders dan 200 POST /orders/{id}/cancel (K14). */
public class PesananDto {

    public static final String MENUNGGU_DIAMBIL = "pending_pickup";
    public static final String SELESAI = "completed";
    public static final String DIBATALKAN = "cancelled";
    public static final String TIDAK_DIAMBIL = "no_show";

    @SerializedName("id")
    public long id;

    /** Nomor pesanan, misalnya LOF-7Q2K9A. Bukan kode yang ditukar di toko. */
    @SerializedName("code")
    public String code;

    @SerializedName("status")
    public String status;

    /** Kode yang ditunjukkan di toko. Null kalau sudah tidak bisa ditukar. */
    @Nullable
    @SerializedName("pickup_code")
    public String pickupCode;

    @Nullable
    @SerializedName("pickup_code_status")
    public String pickupCodeStatus;

    @SerializedName("pickup_start")
    public String pickupStart;

    @SerializedName("pickup_end")
    public String pickupEnd;

    @SerializedName("store")
    public PratinjauPesananDto.Toko store;

    @SerializedName("items")
    public List<PratinjauPesananDto.Baris> items;

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

    @SerializedName("payment_method")
    public String paymentMethod;

    @SerializedName("payment_status")
    public String paymentStatus;

    @Nullable
    @SerializedName("note")
    public String note;

    @SerializedName("placed_at")
    public String placedAt;

    @Nullable
    @SerializedName("completed_at")
    public String completedAt;

    @Nullable
    @SerializedName("cancelled_at")
    public String cancelledAt;

    /** Kode masih bisa ditunjukkan di toko. */
    public boolean kodeAktif() {
        return MENUNGGU_DIAMBIL.equals(status) && pickupCode != null && !pickupCode.isEmpty();
    }
}
