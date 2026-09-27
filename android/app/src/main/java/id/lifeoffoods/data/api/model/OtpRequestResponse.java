package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Respons 202 POST /auth/otp/request. */
public class OtpRequestResponse {

    @SerializedName("message")
    public String message;

    @SerializedName("expires_in")
    public int expiresIn;

    @SerializedName("resend_in")
    public int resendIn;

    /** Hanya ada selama PILOT_MODE=true; K03 menampilkannya dengan spanduk mode uji coba. */
    @Nullable
    @SerializedName("pilot_code")
    public String pilotCode;
}
