package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** M07 Laporan mingguan: GET /partner/stores/{store}/reports/weekly?week_start= (PRD-13). */
public class LaporanMingguanDto {

    @SerializedName("week_start")
    public String weekStart;

    @SerializedName("week_end")
    public String weekEnd;

    @SerializedName("wasted_value_rupiah")
    public long wastedValueRupiah;

    @SerializedName("wasted_weight_gram")
    public long wastedWeightGram;

    @SerializedName("logged_days")
    public int loggedDays;

    /** Uang yang kembali ke mitra dari pesanan yang sudah diambil, bukan nilai asli makanan. */
    @SerializedName("rescued_value_rupiah")
    public long rescuedValueRupiah;

    @SerializedName("orders_count")
    public int ordersCount;

    @SerializedName("items_sold")
    public int itemsSold;

    /** Terbuang + terselamatkan. */
    @SerializedName("unsold_value_rupiah")
    public long unsoldValueRupiah;

    /** Nilai terbuang dibanding minggu lalu; null kalau minggu lalu tidak dicatat. */
    @Nullable
    @SerializedName("wasted_change_percent")
    public Integer wastedChangePercent;

    @Nullable
    @SerializedName("top_wasted_products")
    public List<Produk> topWastedProducts;

    @Nullable
    @SerializedName("daily")
    public List<Harian> daily;

    public static class Produk {
        @Nullable
        @SerializedName("product_id")
        public Long productId;

        @Nullable
        @SerializedName("label")
        public String label;

        @SerializedName("qty")
        public int qty;

        @Nullable
        @SerializedName("weight_gram")
        public Long weightGram;

        @SerializedName("value_rupiah")
        public long valueRupiah;
    }

    public static class Harian {
        @SerializedName("date")
        public String date;

        /** null = hari itu tidak dicatat (bukan 0). */
        @Nullable
        @SerializedName("wasted_value_rupiah")
        public Long wastedValueRupiah;
    }
}
