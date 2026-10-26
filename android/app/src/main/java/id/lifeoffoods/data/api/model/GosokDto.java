package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** POST /games/scratch: hasil Gosok & Menang hari ini. */
public class GosokDto {
    @SerializedName("won")
    public boolean won;

    @Nullable
    @SerializedName("voucher")
    public VoucherDto voucher;
}
