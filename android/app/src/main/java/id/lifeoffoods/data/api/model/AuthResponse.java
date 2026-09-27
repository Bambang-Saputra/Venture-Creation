package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** Respons POST /auth/otp/verify dan POST /auth/google (tidak dibungkus data). */
public class AuthResponse {

    @SerializedName("token")
    public String token;

    /** true: buka K04 Lengkapi profil, bukan beranda. */
    @SerializedName("is_new_user")
    public boolean isNewUser;

    @SerializedName("user")
    public UserDto user;
}
