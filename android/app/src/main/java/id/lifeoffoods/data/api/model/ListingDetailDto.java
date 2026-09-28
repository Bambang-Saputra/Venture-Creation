package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** GET /listings/{id}: semua field ringkas ditambah isi detail (K10, K11). */
public class ListingDetailDto extends ListingDto {

    @Nullable
    @SerializedName("description")
    public String description;

    /** Perkiraan isi tas, misalnya "2-3 pastry campur". */
    @Nullable
    @SerializedName("content_hint")
    public String contentHint;

    @Nullable
    @SerializedName("ingredients_text")
    public String ingredientsText;

    @Nullable
    @SerializedName("halal_certificate_no")
    public String halalCertificateNo;

    @SerializedName("status")
    public String status;

    /** false: habis, lewat jam ambil, dijeda, atau toko tutup sementara. Matikan tombol pesan. */
    @SerializedName("is_available")
    public boolean isAvailable;

    @Nullable
    @SerializedName("items")
    public List<Item> items;

    public static class Item {
        @SerializedName("label")
        public String label;

        @Nullable
        @SerializedName("qty")
        public Integer qty;
    }
}
