package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Body POST /partner/stores/{store}/listings. Tas kejutan (M09) memakai {@code template_id} atau,
 * untuk "tas campur", {@code title} dan {@code price_rupiah}. Menu satuan (M16) memakai {@code
 * items}, satu produk menjadi satu jualan. Field null tidak dikirim (Gson).
 */
public class JualanBody {

    @SerializedName("type")
    public String type;

    /** "HH:mm". */
    @SerializedName("pickup_start")
    public String pickupStart;

    @SerializedName("pickup_end")
    public String pickupEnd;

    @SerializedName("publish")
    public boolean publish = true;

    /**
     * {@code certified}, {@code self_claim}, atau {@code not_stated}. Null = bawaan cetakan/toko.
     */
    @Nullable
    @SerializedName("halal_label")
    public String halalLabel;

    // Tas kejutan
    @Nullable
    @SerializedName("template_id")
    public Long templateId;

    @Nullable
    @SerializedName("title")
    public String title;

    @Nullable
    @SerializedName("price_rupiah")
    public Long priceRupiah;

    @Nullable
    @SerializedName("original_value_rupiah")
    public Long originalValueRupiah;

    @Nullable
    @SerializedName("content_hint")
    public String contentHint;

    @Nullable
    @SerializedName("ingredients_text")
    public String ingredientsText;

    @Nullable
    @SerializedName("qty_total")
    public Integer qtyTotal;

    @Nullable
    @SerializedName("allergens")
    public List<JualanMitraDto.Alergen> allergens;

    // Menu satuan
    @Nullable
    @SerializedName("items")
    public List<Item> items;

    public static class Item {
        @SerializedName("product_id")
        public long productId;

        @SerializedName("qty_total")
        public int qtyTotal;

        @SerializedName("price_rupiah")
        public long priceRupiah;

        @SerializedName("allergens")
        public List<JualanMitraDto.Alergen> allergens;

        public Item(
                long productId,
                int qtyTotal,
                long priceRupiah,
                List<JualanMitraDto.Alergen> alergen) {
            this.productId = productId;
            this.qtyTotal = qtyTotal;
            this.priceRupiah = priceRupiah;
            this.allergens = alergen;
        }
    }
}
