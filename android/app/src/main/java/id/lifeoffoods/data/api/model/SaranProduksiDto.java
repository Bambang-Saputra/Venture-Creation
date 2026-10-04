package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** GET /partner/stores/{store}/suggestions (M08 Saran produksi). Hanya pemilik. */
public class SaranProduksiDto {

    public static final String BARU = "new";
    public static final String DIPAKAI = "accepted";
    public static final String DIABAIKAN = "dismissed";

    /** yyyy-MM-dd, bawaan besok. */
    @SerializedName("suggested_for_date")
    public String suggestedForDate;

    /** "Sabtu". */
    @SerializedName("weekday")
    public String weekday;

    @SerializedName("sample_days")
    public int sampleDays;

    /** false: catatan kurang dari 3 hari yang sama; tampilkan ajakan mencatat. */
    @SerializedName("has_enough_data")
    public boolean hasEnoughData;

    /** Produk aktif yang belum diisi jumlah produksi hariannya, jadi tidak diberi saran. */
    @SerializedName("products_missing_production_qty")
    public int productsMissingProductionQty;

    /** Jumlah hemat per minggu dari saran yang tidak diabaikan. */
    @SerializedName("total_saving_per_week_rupiah")
    public long totalSavingPerWeekRupiah;

    @SerializedName("items")
    public List<Butir> items;

    public static class Butir {
        @SerializedName("id")
        public long id;

        @SerializedName("name")
        public String name;

        /** "potong", "pcs", ... */
        @Nullable
        @SerializedName("unit")
        public String unit;

        @SerializedName("current_production")
        public int currentProduction;

        @SerializedName("suggested_production")
        public int suggestedProduction;

        @SerializedName("reduce_by")
        public int reduceBy;

        @SerializedName("avg_waste_qty")
        public double avgWasteQty;

        @SerializedName("estimated_saving_per_week_rupiah")
        public long estimatedSavingPerWeekRupiah;

        /** new, accepted, dismissed. */
        @SerializedName("status")
        public String status;
    }

    /** Jawaban POST .../suggestions/{id}/accept|dismiss. */
    public static class Tanggapan {
        @SerializedName("id")
        public long id;

        @SerializedName("status")
        public String status;
    }
}
