package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/** Objek user di respons auth dan GET /me. */
public class UserDto {

    @SerializedName("id")
    public long id;

    @Nullable
    @SerializedName("name")
    public String name;

    @Nullable
    @SerializedName("email")
    public String email;

    /** Selalu ternormalisasi 628xxxxxxxxx; null untuk akun yang hanya masuk lewat Google. */
    @Nullable
    @SerializedName("phone")
    public String phone;

    /** consumer atau partner. */
    @SerializedName("role")
    public String role;

    /** Hanya ada di GET /me. */
    @SerializedName("has_google")
    public boolean hasGoogle;

    /** Foto profil (K19); null kalau belum ada. Hanya di GET /me. */
    @SerializedName("photo_url")
    public String photoUrl;
}
