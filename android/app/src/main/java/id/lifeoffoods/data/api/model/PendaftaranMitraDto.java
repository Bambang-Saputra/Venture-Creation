package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * M03/M04: pendaftaran toko oleh akun mitra baru, dari GET dan POST /partner/application. Toko baru
 * dibuat saat tim menyetujuinya lewat {@code php artisan mitra:setujui}.
 */
public class PendaftaranMitraDto {

    public static final String MENUNGGU = "pending";
    public static final String DISETUJUI = "approved";
    public static final String DITOLAK = "rejected";

    @SerializedName("id")
    public long id;

    /** {@link #MENUNGGU}, {@link #DISETUJUI}, atau {@link #DITOLAK}. */
    @SerializedName("status")
    public String status;

    @SerializedName("owner_name")
    public String ownerName;

    @SerializedName("store_name")
    public String storeName;

    @SerializedName("category")
    public String category;

    @SerializedName("address")
    public String address;

    /** HH:mm */
    @SerializedName("open_time")
    public String openTime;

    @SerializedName("close_time")
    public String closeTime;

    @Nullable
    @SerializedName("nib")
    public String nib;

    @Nullable
    @SerializedName("halal_certificate_no")
    public String halalCertificateNo;

    /** Alasan dari tim; hanya terisi kalau {@link #DITOLAK}. */
    @Nullable
    @SerializedName("rejection_reason")
    public String rejectionReason;

    @SerializedName("submitted_at")
    public String submittedAt;

    /** Badan POST /partner/application. NIB dan sertifikat halal dikirim null kalau kosong. */
    public static class Kirim {

        @SerializedName("owner_name")
        public String ownerName;

        @SerializedName("store_name")
        public String storeName;

        @SerializedName("category")
        public String category;

        @SerializedName("address")
        public String address;

        @SerializedName("open_time")
        public String openTime;

        @SerializedName("close_time")
        public String closeTime;

        @Nullable
        @SerializedName("nib")
        public String nib;

        @Nullable
        @SerializedName("halal_certificate_no")
        public String halalCertificateNo;
    }
}
