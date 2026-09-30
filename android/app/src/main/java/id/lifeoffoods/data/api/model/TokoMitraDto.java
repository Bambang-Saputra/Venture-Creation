package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** Satu toko dari GET /partner/stores (dipanggil setelah M02 untuk mendapatkan store_id). */
public class TokoMitraDto {

    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    /** {@code owner} atau {@code cashier}. */
    @SerializedName("my_role")
    public String myRole;
}
