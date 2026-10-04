package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/** GET /notifications: titik lonceng K07 ({@code unread_count}) dan daftar K17, 20 per halaman. */
public class NotifikasiResponse {

    @SerializedName("unread_count")
    public int unreadCount;

    @Nullable
    @SerializedName("data")
    public List<Notifikasi> data;

    @SerializedName("current_page")
    public int currentPage;

    @SerializedName("last_page")
    public int lastPage;

    public boolean adaBerikutnya() {
        return currentPage > 0 && currentPage < lastPage;
    }

    /** Satu notifikasi. */
    public static class Notifikasi {
        public static final String SIAP_DIAMBIL = "pengingat_ambil";
        public static final String MITRA_FAVORIT = "mitra_favorit_memasang";
        public static final String PORSI = "porsi_terselamatkan";
        public static final String PESANAN_BARU = "pesanan_baru";

        @SerializedName("id")
        public long id;

        @SerializedName("type")
        public String type;

        @SerializedName("title")
        public String title;

        @Nullable
        @SerializedName("body")
        public String body;

        /** Tujuan ketukan, contoh {"screen":"K14","order_id":88}. */
        @Nullable
        @SerializedName("data")
        public Map<String, Object> data;

        @SerializedName("is_read")
        public boolean isRead;

        @SerializedName("created_at")
        public String createdAt;

        /** id pesanan untuk notifikasi yang membuka K14, atau 0. */
        public long idPesanan() {
            Object v = data == null ? null : data.get("order_id");
            return v instanceof Number ? ((Number) v).longValue() : 0;
        }
    }
}
