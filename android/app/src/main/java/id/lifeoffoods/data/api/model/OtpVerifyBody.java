package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** POST /auth/otp/verify. */
public class OtpVerifyBody {

    @SerializedName("phone")
    public final String phone;

    @SerializedName("code")
    public final String code;

    @SerializedName("role")
    public final String role;

    public OtpVerifyBody(String phone, String code, String role) {
        this.phone = phone;
        this.code = code;
        this.role = role;
    }
}
