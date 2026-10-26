package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Voucher pembeli (GET /vouchers, ADR-0008). Untuk "mine", {@code id} adalah id voucher milik
 * pembeli yang dikirim sebagai {@code user_voucher_id}; untuk "promo", {@code id} adalah id voucher
 * yang bisa diklaim.
 */
public class VoucherDto {
    public static final String PERSEN = "percent";

    @SerializedName("id")
    public long id;

    @SerializedName("code")
    public String code;

    @SerializedName("title")
    public String title;

    @SerializedName("type")
    public String type;

    @SerializedName("value")
    public long value;

    @Nullable
    @SerializedName("max_discount_rupiah")
    public Long maxDiscountRupiah;

    @SerializedName("min_spend_rupiah")
    public long minSpendRupiah;

    @Nullable
    @SerializedName("expires_at")
    public String expiresAt;

    @Nullable
    @SerializedName("valid_days")
    public Integer validDays;

    public boolean persen() {
        return PERSEN.equals(type);
    }
}
