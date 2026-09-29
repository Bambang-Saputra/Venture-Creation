package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Body POST /orders/preview (K12/K13 saat dibuka) dan POST /orders (tombol "Buat pesanan"). Preview
 * mengabaikan {@code note} dan {@code payment_method}; Gson tidak mengirim field null.
 */
public class PesananBody {

    public static final String BAYAR_TUNAI = "cash";
    public static final String BAYAR_QRIS = "qris_static";
    public static final int MAKS_CATATAN = 300;

    @SerializedName("items")
    public final List<Item> items = new ArrayList<>();

    @Nullable
    @SerializedName("note")
    public String note;

    @Nullable
    @SerializedName("payment_method")
    public String paymentMethod;

    /** Dari argumen navigasi K10/K11: {@code id_jualan} dan {@code jumlah} sejajar. */
    public static PesananBody dari(long[] idJualan, int[] jumlah) {
        if (idJualan == null || jumlah == null || idJualan.length != jumlah.length) {
            throw new IllegalArgumentException("id_jualan dan jumlah harus sama panjang");
        }
        PesananBody body = new PesananBody();
        for (int i = 0; i < idJualan.length; i++) {
            if (jumlah[i] > 0) {
                body.items.add(new Item(idJualan[i], jumlah[i]));
            }
        }
        return body;
    }

    /** Salinan untuk POST /orders. Catatan kosong tidak dikirim, catatan panjang dipotong. */
    public PesananBody untukDibuat(@Nullable String catatan, String metodeBayar) {
        PesananBody body = new PesananBody();
        body.items.addAll(items);
        String rapi = catatan == null ? "" : catatan.trim();
        if (rapi.length() > MAKS_CATATAN) {
            rapi = rapi.substring(0, MAKS_CATATAN);
        }
        body.note = rapi.isEmpty() ? null : rapi;
        body.paymentMethod = metodeBayar;
        return body;
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
