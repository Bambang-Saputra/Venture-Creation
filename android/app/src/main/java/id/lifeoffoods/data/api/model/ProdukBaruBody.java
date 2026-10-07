package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** Body POST /partner/stores/{store}/products, "Tambah menu baru" di M16. */
public class ProdukBaruBody {

    @SerializedName("name")
    public final String name;

    /** Harga normal; jadi harga coret saat dijual murah. */
    @SerializedName("price_rupiah")
    public final long priceRupiah;

    @SerializedName("ingredients_text")
    public final String ingredientsText;

    public ProdukBaruBody(String name, long priceRupiah, String ingredientsText) {
        this.name = name;
        this.priceRupiah = priceRupiah;
        this.ingredientsText = ingredientsText;
    }
}
