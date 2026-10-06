package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Satu iklan toko (GET/POST /partner/stores/{store}/promotions). */
public class PromosiDto {

    public static final String PRIORITAS = "search_priority";
    public static final String BANNER = "home_banner";

    @SerializedName("id")
    public long id;

    @SerializedName("package")
    public String paket;

    @Nullable
    @SerializedName("headline")
    public String headline;

    /** yyyy-MM-dd. */
    @SerializedName("starts_on")
    public String startsOn;

    @SerializedName("ends_on")
    public String endsOn;

    @SerializedName("days")
    public int days;

    @SerializedName("total_rupiah")
    public long totalRupiah;

    /** active, scheduled, atau ended. */
    @SerializedName("status")
    public String status;

    /** Jawaban GET: daftar paket dengan tarif, status tagihan, dan iklan toko. */
    public static class Halaman {
        @Nullable
        @SerializedName("packages")
        public List<Paket> packages;

        @Nullable
        @SerializedName("billing")
        public Tagihan billing;

        @Nullable
        @SerializedName("data")
        public List<PromosiDto> data;
    }

    public static class Paket {
        @SerializedName("code")
        public String code;

        @SerializedName("name")
        public String name;

        @SerializedName("description")
        public String description;

        @SerializedName("price_per_day_rupiah")
        public long pricePerDayRupiah;
    }

    public static class Tagihan {
        @SerializedName("enabled")
        public boolean enabled;

        @Nullable
        @SerializedName("reason")
        public String reason;
    }

    public static class Body {
        @SerializedName("package")
        public final String paket;

        @SerializedName("days")
        public final int days;

        @Nullable
        @SerializedName("headline")
        public final String headline;

        public Body(String paket, int days, @Nullable String headline) {
            this.paket = paket;
            this.days = days;
            this.headline = headline;
        }
    }
}
