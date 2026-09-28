package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** PUT /me/allergens. Mengganti seluruh pilihan; daftar kosong berarti mengosongkan. */
public class AlergenBody {

    @SerializedName("allergens")
    public final List<Pilihan> allergens = new ArrayList<>();

    /** Semua kode dengan severity bawaan server ({@code avoid}); K05 tidak menanyakan tingkat. */
    public static AlergenBody dariKode(Collection<String> kode) {
        AlergenBody body = new AlergenBody();
        for (String k : kode) {
            body.allergens.add(new Pilihan(k, null));
        }
        return body;
    }

    public static class Pilihan {
        @SerializedName("code")
        public final String code;

        /** avoid atau severe; null memakai avoid. */
        @Nullable
        @SerializedName("severity")
        public final String severity;

        public Pilihan(String code, @Nullable String severity) {
            this.code = code;
            this.severity = severity;
        }
    }
}
