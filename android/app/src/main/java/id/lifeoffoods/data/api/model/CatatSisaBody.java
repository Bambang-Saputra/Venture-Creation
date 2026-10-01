package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Body POST /partner/stores/{store}/waste-logs. Mengirim lagi di hari yang sama mengganti isinya.
 * {@code per_item} wajib {@code qty}; {@code weight} wajib {@code weight_gram}.
 */
public class CatatSisaBody {

    @SerializedName("log_date")
    public final String logDate;

    @SerializedName("method")
    public final String method;

    @Nullable
    @SerializedName("note")
    public String note;

    @SerializedName("items")
    public final List<Item> items;

    public CatatSisaBody(String logDate, String method, List<Item> items) {
        this.logDate = logDate;
        this.method = method;
        this.items = items;
    }

    /** Produk ({@code product_id}) atau item lain ({@code label} dan {@code unit_value_rupiah}). */
    public static class Item {
        @Nullable
        @SerializedName("product_id")
        public Long productId;

        @Nullable
        @SerializedName("label")
        public String label;

        @Nullable
        @SerializedName("qty")
        public Integer qty;

        @Nullable
        @SerializedName("weight_gram")
        public Integer weightGram;

        @Nullable
        @SerializedName("unit_value_rupiah")
        public Long unitValueRupiah;

        /**
         * {@code discarded} (bawaan), {@code donated}, {@code staff_meal}, {@code sold_surplus}.
         */
        @SerializedName("disposition")
        public String disposition;
    }
}
