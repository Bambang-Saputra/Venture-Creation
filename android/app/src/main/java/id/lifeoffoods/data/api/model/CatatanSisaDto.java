package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Catatan sisa satu hari: GET /partner/stores/{store}/waste-logs?date= dan respons 200 POST yang
 * sama (M06, M19). {@code products} berisi semua produk aktif, termasuk yang sisanya 0.
 */
public class CatatanSisaDto {

    public static final String PER_ITEM = "per_item";
    public static final String TIMBANG = "weight";

    @SerializedName("log_date")
    public String logDate;

    @SerializedName("is_recorded")
    public boolean isRecorded;

    /** Lewat akhir hari berikutnya: hanya bisa dibaca (POST dijawab 409). */
    @SerializedName("is_locked")
    public boolean isLocked;

    @SerializedName("method")
    public String method;

    @Nullable
    @SerializedName("note")
    public String note;

    @SerializedName("total_value_rupiah")
    public long totalValueRupiah;

    @SerializedName("total_weight_gram")
    public long totalWeightGram;

    @SerializedName("total_items")
    public int totalItems;

    /** Dibanding hari yang sama pekan lalu; null kalau pekan lalu tidak dicatat. */
    @Nullable
    @SerializedName("change_vs_last_week_percent")
    public Integer changeVsLastWeekPercent;

    @Nullable
    @SerializedName("products")
    public List<Produk> products;

    @Nullable
    @SerializedName("other_items")
    public List<ItemLain> otherItems;

    public static class Produk {
        @SerializedName("product_id")
        public long productId;

        @SerializedName("name")
        public String name;

        @Nullable
        @SerializedName("unit")
        public String unit;

        @SerializedName("price_rupiah")
        public long priceRupiah;

        /** HPP, atau harga jual kalau HPP kosong. */
        @SerializedName("unit_value_rupiah")
        public long unitValueRupiah;

        @SerializedName("qty")
        public int qty;

        @Nullable
        @SerializedName("weight_gram")
        public Integer weightGram;

        @Nullable
        @SerializedName("disposition")
        public String disposition;
    }

    public static class ItemLain {
        @SerializedName("label")
        public String label;

        @Nullable
        @SerializedName("qty")
        public Integer qty;

        @Nullable
        @SerializedName("weight_gram")
        public Integer weightGram;

        @SerializedName("value_rupiah")
        public long valueRupiah;
    }
}
