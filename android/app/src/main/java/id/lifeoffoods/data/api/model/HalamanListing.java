package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Satu halaman GET /listings (paginator Laravel): isi plus total seluruh hasil filter. */
public class HalamanListing {

    @SerializedName("data")
    public List<ListingDto> data;

    @SerializedName("total")
    public int total;
}
