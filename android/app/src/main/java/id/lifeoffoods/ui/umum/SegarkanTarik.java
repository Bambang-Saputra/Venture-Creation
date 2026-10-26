package id.lifeoffoods.ui.umum;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import id.lifeoffoods.R;

/**
 * Tarik ke bawah untuk memuat ulang di layar daftar (K15, K17, M05, M11). Daftar yang sudah ada
 * dibungkus SwipeRefreshLayout saat runtime, jadi layout tiap layar tidak perlu diubah dan view
 * binding tetap menunjuk ke view yang sama.
 */
public final class SegarkanTarik {

    private SegarkanTarik() {}

    public static SwipeRefreshLayout bungkus(View gulir, Runnable muatUlang) {
        ViewGroup induk = (ViewGroup) gulir.getParent();
        int posisi = induk.indexOfChild(gulir);
        ViewGroup.LayoutParams lp = gulir.getLayoutParams();
        induk.removeViewAt(posisi);

        SwipeRefreshLayout s = new SwipeRefreshLayout(gulir.getContext());
        s.setId(View.generateViewId());
        s.addView(
                gulir,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        induk.addView(s, posisi, lp);
        s.setColorSchemeResources(R.color.merek_utama, R.color.war_aksen);
        s.setOnRefreshListener(muatUlang::run);
        return s;
    }

    /** Hentikan putaran begitu status layar bukan lagi MEMUAT (berhasil atau gagal). */
    public static void selesai(SwipeRefreshLayout s, @Nullable Enum<?> status) {
        if (status == null || !"MEMUAT".equals(status.name())) {
            s.setRefreshing(false);
        }
    }
}
