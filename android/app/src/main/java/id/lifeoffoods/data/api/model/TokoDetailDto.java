package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** GET /partner/stores/{store} (M14 Profil toko). Pemilik dan kasir. */
public class TokoDetailDto {

    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    /** cafe, bakery, resto, catering, grocery. */
    @SerializedName("category")
    public String category;

    @SerializedName("address")
    public String address;

    /** Hanya ada setelah fitur unggah foto (F-20) masuk ke server; null kalau belum ada foto. */
    @Nullable
    @SerializedName("photo_url")
    public String photoUrl;

    /** certified, self_claim, not_stated. */
    @SerializedName("halal_label")
    public String halalLabel;

    @Nullable
    @SerializedName("default_ingredients_text")
    public String defaultIngredientsText;

    @SerializedName("is_temporarily_closed")
    public boolean isTemporarilyClosed;

    @SerializedName("hours")
    public List<JamToko> hours;

    /** owner atau cashier. */
    @SerializedName("my_role")
    public String myRole;

    /** null untuk kasir: kasir tidak boleh melihat saldo (M15). */
    @Nullable
    @SerializedName("available_balance_rupiah")
    public Long availableBalanceRupiah;

    /** null kalau belum ada ulasan. */
    @Nullable
    @SerializedName("rating_average")
    public Double ratingAverage;

    @SerializedName("rating_count")
    public int ratingCount;

    public boolean pemilik() {
        return "owner".equals(myRole);
    }

    /** Satu hari jam operasional. */
    public static class JamToko {

        /** 0 Minggu sampai 6 Sabtu, sama dengan Carbon dayOfWeek. */
        @SerializedName("day_of_week")
        public int dayOfWeek;

        @SerializedName("is_closed")
        public boolean isClosed;

        /** "07:00", null kalau tutup. */
        @Nullable
        @SerializedName("open_time")
        public String openTime;

        @Nullable
        @SerializedName("close_time")
        public String closeTime;
    }

    /** PATCH /partner/stores/{store}. Gson melewatkan field null, jadi kirim yang berubah saja. */
    public static class Ubah {

        @Nullable
        @SerializedName("is_temporarily_closed")
        public Boolean isTemporarilyClosed;
    }
}
