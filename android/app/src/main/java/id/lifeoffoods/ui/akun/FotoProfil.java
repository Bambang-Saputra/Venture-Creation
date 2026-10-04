package id.lifeoffoods.ui.akun;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.MeResponse;
import java.util.List;

/**
 * Bantuan tampilan akun K18-K20. Memperkecil foto ada di {@link id.lifeoffoods.ui.umum.FotoUnggah}.
 */
final class FotoProfil {

    private FotoProfil() {}

    /** "Kacang tanah, Susu" atau "Kacang tanah, Susu +2" untuk baris Alergi K18-K20. */
    static String ringkasAlergi(@Nullable List<MeResponse.Alergen> daftar, String kosong) {
        if (daftar == null || daftar.isEmpty()) {
            return kosong;
        }
        StringBuilder sb = new StringBuilder();
        int tampil = Math.min(2, daftar.size());
        for (int i = 0; i < tampil; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(daftar.get(i).name);
        }
        if (daftar.size() > tampil) {
            sb.append(" +").append(daftar.size() - tampil);
        }
        return sb.toString();
    }
}
