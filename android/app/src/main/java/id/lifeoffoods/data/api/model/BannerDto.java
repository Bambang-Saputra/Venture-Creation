package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Satu kartu karusel iklan di atas K07 (GET /promotions/banners). Selalu berlabel "Iklan". */
public class BannerDto {

    @SerializedName("store_id")
    public long storeId;

    @SerializedName("store_name")
    public String storeName;

    @Nullable
    @SerializedName("store_category")
    public String storeCategory;

    /** Kalimat promo dari mitra; null kalau mitra tidak mengisinya. */
    @Nullable
    @SerializedName("headline")
    public String headline;

    @Nullable
    @SerializedName("photo_url")
    public String photoUrl;

    /** Jualan termurah toko ini yang masih bisa dibeli; ketuk banner membukanya. */
    @SerializedName("listing_id")
    public long listingId;

    /** surprise_bag (buka K10) atau menu_item (buka K11). */
    @SerializedName("listing_type")
    public String listingType;

    @SerializedName("price_from_rupiah")
    public long priceFromRupiah;
}
