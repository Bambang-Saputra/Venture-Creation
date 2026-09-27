package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** POST /auth/google. */
public class GoogleLoginBody {

    /** ID token dari Credential Manager; server yang memverifikasinya. */
    @SerializedName("id_token")
    public final String idToken;

    @SerializedName("role")
    public final String role;

    public GoogleLoginBody(String idToken, String role) {
        this.idToken = idToken;
        this.role = role;
    }
}
