package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** Body POST /pickup-codes/redeem (M12). Huruf kecil diterima server, tapi dikirim kapital. */
public class TukarKodeBody {

    @SerializedName("store_id")
    public final long storeId;

    @SerializedName("code")
    public final String code;

    public TukarKodeBody(long storeId, String code) {
        this.storeId = storeId;
        this.code = code;
    }
}
