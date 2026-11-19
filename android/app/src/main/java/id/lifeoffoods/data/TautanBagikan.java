package id.lifeoffoods.data;

import android.net.Uri;
import androidx.annotation.Nullable;
import id.lifeoffoods.BuildConfig;
import java.util.List;

/**
 * Tautan bagikan jualan: {@code <host API>/j/{id}}. Server menampilkan halaman berpratinjau
 * (BagikanController), dan aplikasi yang terpasang membuka K10/K11 lewat intent-filter di manifest.
 * Menu satuan diberi {@code ?jenis=menu} supaya dibuka di K11.
 */
public final class TautanBagikan {

    public static final String JENIS_MENU = "menu";

    private TautanBagikan() {}

    public static String jualan(long id, boolean menu) {
        // API_BASE_URL berakhiran "/api/"; halaman bagikan ada di akar host yang sama.
        String akar = BuildConfig.API_BASE_URL.replaceFirst("api/?$", "");
        if (!akar.endsWith("/")) {
            akar += "/";
        }
        return akar + "j/" + id + (menu ? "?jenis=" + JENIS_MENU : "");
    }

    /** Id jualan dari tautan {@code /j/{id}}, atau null kalau bukan tautan bagikan. */
    @Nullable
    public static Long idDari(@Nullable Uri uri) {
        if (uri == null) {
            return null;
        }
        List<String> bagian = uri.getPathSegments();
        if (bagian.size() != 2 || !"j".equals(bagian.get(0))) {
            return null;
        }
        try {
            return Long.parseLong(bagian.get(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean menu(@Nullable Uri uri) {
        return uri != null && JENIS_MENU.equals(uri.getQueryParameter("jenis"));
    }
}
