package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Produk toko dari GET /partner/stores/{store}/products (M16). Didaftarkan tim selama pilot. */
public class ProdukDto {

    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    @Nullable
    @SerializedName("unit")
    public String unit;

    /** Harga normal; dipakai sebagai harga coret kalau harga jual lebih rendah. */
    @SerializedName("price_rupiah")
    public long priceRupiah;

    @Nullable
    @SerializedName("ingredients_text")
    public String ingredientsText;
}
