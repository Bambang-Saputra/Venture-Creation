package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * GET /partner/stores/{store}/summary: M05 Dashboard mitra (PRD-24) dan pil "Tutup 21.00" di M06.
 * Pemilik dan kasir sama-sama boleh membukanya.
 */
public class RingkasanTokoDto {

    @Nullable
    @SerializedName("store")
    public Toko store;

    @SerializedName("is_open")
    public boolean isOpen;

    /** "21:00"; null kalau hari ini tutup atau jam belum diatur. */
    @Nullable
    @SerializedName("closes_at")
    public String closesAt;

    /** null kalau toko sudah tutup atau tutup hari ini. */
    @Nullable
    @SerializedName("minutes_until_close")
    public Integer minutesUntilClose;

    @SerializedName("is_today_logged")
    public boolean isTodayLogged;

    /** Uang dari pesanan yang sudah diambil minggu ini, sama dengan M07. */
    @SerializedName("rescued_value_this_week_rupiah")
    public long rescuedValueThisWeekRupiah;

    @SerializedName("items_sold_this_week")
    public int itemsSoldThisWeek;

    /** Rata-rata berat sisa per hari yang dicatat, 7 hari terakhir; null kalau belum ada. */
    @Nullable
    @SerializedName("avg_daily_waste_gram")
    public Long avgDailyWasteGram;

    @SerializedName("unsold_bags_last_7_days")
    public int unsoldBagsLast7Days;

    @SerializedName("pending_orders_today")
    public int pendingOrdersToday;

    /** Senin sampai Minggu pekan ini; nilai null = hari itu tidak dicatat. */
    @Nullable
    @SerializedName("daily")
    public List<LaporanMingguanDto.Harian> daily;

    /** "Sabtu"; null kalau belum ada hari yang dicatat. */
    @Nullable
    @SerializedName("peak_day")
    public String peakDay;

    public static class Toko {
        @SerializedName("id")
        public long id;

        @SerializedName("name")
        public String name;
    }
}
