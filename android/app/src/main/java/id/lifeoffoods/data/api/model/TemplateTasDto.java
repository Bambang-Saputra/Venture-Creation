package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Cetakan tas kejutan dari GET /partner/stores/{store}/templates (M09). */
public class TemplateTasDto {

    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    @Nullable
    @SerializedName("content_hint")
    public String contentHint;

    @SerializedName("price_rupiah")
    public long priceRupiah;

    @Nullable
    @SerializedName("original_value_rupiah")
    public Long originalValueRupiah;

    @SerializedName("default_qty")
    public int defaultQty;

    /** Kolom TIME, misalnya "19:00:00". */
    @Nullable
    @SerializedName("pickup_start_time")
    public String pickupStartTime;

    @Nullable
    @SerializedName("pickup_end_time")
    public String pickupEndTime;

    @Nullable
    @SerializedName("halal_label")
    public String halalLabel;
}
