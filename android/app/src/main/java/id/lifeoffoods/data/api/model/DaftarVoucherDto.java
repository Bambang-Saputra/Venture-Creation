package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** GET /vouchers: milikku, voucher pop up yang bisa diklaim, dan status Gosok & Menang. */
public class DaftarVoucherDto {
    @Nullable
    @SerializedName("mine")
    public List<VoucherDto> mine;

    @Nullable
    @SerializedName("promo")
    public VoucherDto promo;

    @SerializedName("can_scratch_today")
    public boolean canScratchToday;
}
