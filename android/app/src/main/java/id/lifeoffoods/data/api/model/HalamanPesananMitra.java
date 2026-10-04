package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Halaman GET /partner/stores/{store}/orders (paginasi Laravel, 30 per halaman). */
public class HalamanPesananMitra {

    @SerializedName("data")
    public List<PesananMitraDto> data;

    @SerializedName("current_page")
    public int currentPage;

    @SerializedName("last_page")
    public int lastPage;

    /** Hanya untuk status=history (M21): hitungan seluruh rentang, bukan hanya halaman ini. */
    @Nullable
    @SerializedName("summary")
    public Ringkasan summary;

    public boolean adaBerikutnya() {
        return currentPage > 0 && currentPage < lastPage;
    }

    public static class Ringkasan {
        @SerializedName("completed")
        public int completed;

        @SerializedName("no_show")
        public int noShow;

        @SerializedName("cancelled")
        public int cancelled;
    }
}
