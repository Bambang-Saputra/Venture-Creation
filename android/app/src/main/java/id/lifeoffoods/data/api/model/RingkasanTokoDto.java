package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * GET /partner/stores/{store}/summary. Baru sebagian: M06 hanya memakai jam tutup. Field lain
 * (penjualan minggu ini, grafik harian) ditambahkan waktu M05 Dashboard dibuat.
 */
public class RingkasanTokoDto {

    @SerializedName("is_open")
    public boolean isOpen;

    /** "21:00"; null kalau hari ini tutup atau jam belum diatur. */
    @Nullable
    @SerializedName("closes_at")
    public String closesAt;

    @SerializedName("is_today_logged")
    public boolean isTodayLogged;
}
