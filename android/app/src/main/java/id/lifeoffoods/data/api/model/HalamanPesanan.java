package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Halaman GET /orders (paginasi Laravel, 20 per halaman). */
public class HalamanPesanan {

    @SerializedName("data")
    public List<PesananRingkasDto> data;

    @SerializedName("current_page")
    public int currentPage;

    @SerializedName("last_page")
    public int lastPage;

    public boolean adaBerikutnya() {
        return currentPage > 0 && currentPage < lastPage;
    }
}
