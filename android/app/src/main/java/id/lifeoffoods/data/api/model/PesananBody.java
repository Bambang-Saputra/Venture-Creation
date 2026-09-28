package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Body POST /orders/preview dan POST /orders. Preview cukup {@code items}; note dan payment_method
 * tidak dikirim (Gson melewati null).
 */
public class PesananBody {

    public static final String BAYAR_TUNAI = "cash";
    public static final String BAYAR_QRIS = "qris_static";

    @SerializedName("items")
    public final List<Item> items = new ArrayList<>();

    @Nullable
    @SerializedName("note")
    public String note;

    @Nullable
    @SerializedName("payment_method")
    public String paymentMethod;

    public static PesananBody dari(Map<Long, Integer> terpilih) {
        PesananBody b = new PesananBody();
        for (Map.Entry<Long, Integer> e : terpilih.entrySet()) {
            b.items.add(new Item(e.getKey(), e.getValue()));
        }
        return b;
    }

    public static class Item {
        @SerializedName("listing_id")
        public final long listingId;

        @SerializedName("qty")
        public final int qty;

        public Item(long listingId, int qty) {
            this.listingId = listingId;
            this.qty = qty;
        }
    }
}
