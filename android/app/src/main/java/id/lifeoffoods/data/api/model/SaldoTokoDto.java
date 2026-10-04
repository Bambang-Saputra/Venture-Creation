package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** M13 Saldo dan pencairan: GET .../balance dan GET .../balance/transactions. Hanya pemilik. */
public class SaldoTokoDto {

    @SerializedName("available_rupiah")
    public long availableRupiah;

    @SerializedName("pending_rupiah")
    public long pendingRupiah;

    @SerializedName("lifetime_rupiah")
    public long lifetimeRupiah;

    /** "Dari 62 tas minggu ini". */
    @SerializedName("items_sold_this_week")
    public int itemsSoldThisWeek;

    @SerializedName("withdrawal")
    public Pencairan withdrawal;

    public static class Pencairan {
        /** false selama pilot (ADR-0004): tombol Cairkan nonaktif dengan alasan. */
        @SerializedName("enabled")
        public boolean enabled;

        @Nullable
        @SerializedName("reason")
        public String reason;
    }

    /** Satu baris Riwayat M13. */
    public static class Transaksi {
        public static final String PENJUALAN = "sale";
        public static final String PENCAIRAN = "withdrawal";

        @SerializedName("id")
        public long id;

        /** sale, adjustment, withdrawal, service_fee. */
        @SerializedName("type")
        public String type;

        /** Negatif untuk pengurangan. */
        @SerializedName("amount_rupiah")
        public long amountRupiah;

        /** "Tas Pastry Sore" atau "Tas Pastry Sore +1 lainnya"; deskripsi kalau bukan pesanan. */
        @Nullable
        @SerializedName("title")
        public String title;

        @Nullable
        @SerializedName("pickup_code")
        public String pickupCode;

        @Nullable
        @SerializedName("description")
        public String description;

        @SerializedName("created_at")
        public String createdAt;
    }

    /** Halaman riwayat, 20 per halaman. */
    public static class HalamanTransaksi {
        @SerializedName("data")
        public List<Transaksi> data;

        @SerializedName("current_page")
        public int currentPage;

        @SerializedName("last_page")
        public int lastPage;

        public boolean adaBerikutnya() {
            return currentPage > 0 && currentPage < lastPage;
        }
    }
}
