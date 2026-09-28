package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/** Satu entri GET /allergens: bahan alergen atau pola makan. */
public class AlergenDto {

    public static final String TIPE_ALERGEN = "allergen";
    public static final String TIPE_DIET = "diet";

    @SerializedName("code")
    public String code;

    @SerializedName("name")
    public String name;

    /** allergen atau diet. */
    @SerializedName("type")
    public String type;
}
