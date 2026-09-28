package id.lifeoffoods.ui.umum;

import android.app.Activity;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import id.lifeoffoods.R;

/**
 * Jarak aman dari status bar, navbar, dan keyboard.
 *
 * <p>targetSdk 35+ membuat Android 15 ke atas selalu edge-to-edge: isi digambar di bawah status bar
 * dan navbar, dan adjustResize tidak lagi mendorong isi saat keyboard muncul. MainActivity
 * menyalakan edge-to-edge di semua versi supaya perilakunya sama, lalu tiap layar memanggil kelas
 * ini untuk view yang menempel ke tepi:
 *
 * <ul>
 *   <li>{@link #atas(View)} untuk app bar atau header, supaya latarnya ikut mengisi area status
 *       bar;
 *   <li>{@link #bawah(View)} untuk bilah tombol bawah atau Nav bawah, termasuk tinggi keyboard.
 * </ul>
 *
 * Padding asli view dipertahankan; inset ditambahkan di atasnya.
 */
public final class SisiAman {

    private SisiAman() {}

    public static void atas(View v) {
        pasang(v, true, false);
    }

    public static void bawah(View v) {
        pasang(v, false, true);
    }

    public static void atasBawah(View v) {
        pasang(v, true, true);
    }

    /**
     * Warna ikon status bar dan navbar: {@code true} untuk ikon gelap di layar terang, {@code
     * false} untuk ikon putih di atas latar hijau K01.
     */
    public static void ikonGelap(Activity a, boolean gelap) {
        var c = WindowCompat.getInsetsController(a.getWindow(), a.getWindow().getDecorView());
        c.setAppearanceLightStatusBars(gelap);
        c.setAppearanceLightNavigationBars(gelap);
    }

    private static void pasang(View v, boolean atas, boolean bawah) {
        Object tag = v.getTag(R.id.tag_padding_asli);
        int[] asli;
        if (tag instanceof int[]) {
            asli = (int[]) tag;
        } else {
            asli =
                    new int[] {
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        v.getPaddingBottom()
                    };
            v.setTag(R.id.tag_padding_asli, asli);
        }
        ViewCompat.setOnApplyWindowInsetsListener(
                v,
                (view, insets) -> {
                    Insets sistem =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());
                    int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                    view.setPadding(
                            asli[0] + sistem.left,
                            asli[1] + (atas ? sistem.top : 0),
                            asli[2] + sistem.right,
                            asli[3] + (bawah ? Math.max(sistem.bottom, ime) : 0));
                    return insets;
                });
        if (v.isAttachedToWindow()) {
            ViewCompat.requestApplyInsets(v);
        } else {
            v.addOnAttachStateChangeListener(
                    new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View view) {
                            view.removeOnAttachStateChangeListener(this);
                            ViewCompat.requestApplyInsets(view);
                        }

                        @Override
                        public void onViewDetachedFromWindow(View view) {}
                    });
        }
    }
}
