package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Jualan dari sisi mitra: GET /partner/stores/{store}/listings (M10, M17), respons 201 POST
 * .../listings, dan 200 POST .../publish atau .../pause.
 */
public class JualanMitraDto {

    public static final String TIPE_TAS = "surprise_bag";
    public static final String TIPE_MENU = "menu_item";

    public static final String DRAF = "draft";
    public static final String AKTIF = "active";
    public static final String DIJEDA = "paused";
    public static final String HABIS = "sold_out";
    public static final String LEWAT = "expired";

    @SerializedName("id")
    public long id;

    /** Foto jualan ini (dari template saat dipasang, atau diunggah di M10), atau null. */
    @androidx.annotation.Nullable
    @SerializedName("photo_url")
    public String photoUrl;

    @SerializedName("type")
    public String type;

    @SerializedName("status")
    public String status;

    @SerializedName("title")
    public String title;

    @Nullable
    @SerializedName("product_id")
    public Long productId;

    @Nullable
    @SerializedName("template_id")
    public Long templateId;

    @SerializedName("price_rupiah")
    public long priceRupiah;

    @Nullable
    @SerializedName("original_value_rupiah")
    public Long originalValueRupiah;

    @SerializedName("qty_total")
    public int qtyTotal;

    @SerializedName("qty_reserved")
    public int qtyReserved;

    @SerializedName("qty_sold")
    public int qtySold;

    @SerializedName("qty_remaining")
    public int qtyRemaining;

    @SerializedName("potential_income_rupiah")
    public long potentialIncomeRupiah;

    @SerializedName("pickup_start")
    public String pickupStart;

    @SerializedName("pickup_end")
    public String pickupEnd;

    @Nullable
    @SerializedName("ingredients_text")
    public String ingredientsText;

    @Nullable
    @SerializedName("halal_label")
    public String halalLabel;

    @Nullable
    @SerializedName("allergens")
    public List<Alergen> allergens;

    /** Alergen jualan. Respons hanya berisi kode; namanya diambil dari GET /allergens. */
    public static class Alergen {
        public static final String MENGANDUNG = "contains";
        public static final String MUNGKIN = "may_contain";

        @SerializedName("code")
        public String code;

        @SerializedName("presence")
        public String presence;

        public Alergen(String code, String presence) {
            this.code = code;
            this.presence = presence;
        }
    }
}
