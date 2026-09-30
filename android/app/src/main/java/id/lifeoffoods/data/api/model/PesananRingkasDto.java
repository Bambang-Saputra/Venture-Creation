package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** Satu baris GET /orders?status=active|history (K15, kontrak API bagian 6). */
public class PesananRingkasDto {

    @SerializedName("id")
    public long id;

    @SerializedName("code")
    public String code;

    /** {@code pending_pickup}, {@code completed}, {@code cancelled}, atau {@code no_show}. */
    @SerializedName("status")
    public String status;

    @SerializedName("store_name")
    public String storeName;

    @SerializedName("item_count")
    public int itemCount;

    @SerializedName("total_rupiah")
    public long totalRupiah;

    @SerializedName("pickup_start")
    public String pickupStart;

    @SerializedName("pickup_end")
    public String pickupEnd;

    @SerializedName("placed_at")
    public String placedAt;
}
