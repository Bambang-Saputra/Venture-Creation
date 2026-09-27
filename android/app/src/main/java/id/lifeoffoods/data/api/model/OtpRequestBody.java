package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** POST /auth/otp/request. */
public class OtpRequestBody {

    @SerializedName("phone")
    public final String phone;

    /** consumer atau partner. */
    @SerializedName("role")
    public final String role;

    public OtpRequestBody(String phone, String role) {
        this.phone = phone;
        this.role = role;
    }
}
