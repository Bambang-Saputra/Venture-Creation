package id.lifeoffoods.data.api.model;

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

    public boolean adaBerikutnya() {
        return currentPage > 0 && currentPage < lastPage;
    }
}
