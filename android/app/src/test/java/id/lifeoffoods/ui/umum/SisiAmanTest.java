package id.lifeoffoods.ui.umum;

import static org.junit.Assert.assertEquals;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Inset sistem ditambahkan di atas padding asli, dan keyboard mengganti tinggi navbar. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class SisiAmanTest {

    private static WindowInsetsCompat inset(int atas, int bawah, int keyboard) {
        return new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, atas, 0, bawah))
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboard))
                .build();
    }

    private static View view() {
        View v = new View(RuntimeEnvironment.getApplication());
        v.setPadding(20, 4, 20, 14);
        return v;
    }

    @Test
    public void atasHanyaMenambahStatusBar() {
        View v = view();
        SisiAman.atas(v);
        ViewCompat.dispatchApplyWindowInsets(v, inset(63, 48, 0));
        assertEquals(4 + 63, v.getPaddingTop());
        assertEquals(14, v.getPaddingBottom());
    }

    @Test
    public void bawahMemakaiNavbarAtauKeyboardYangLebihTinggi() {
        View v = view();
        SisiAman.bawah(v);
        ViewCompat.dispatchApplyWindowInsets(v, inset(63, 48, 0));
        assertEquals(4, v.getPaddingTop());
        assertEquals(14 + 48, v.getPaddingBottom());

        ViewCompat.dispatchApplyWindowInsets(v, inset(63, 48, 900));
        assertEquals(14 + 900, v.getPaddingBottom());
    }

    @Test
    public void insetBerulangTidakMenumpuk() {
        View v = view();
        SisiAman.atasBawah(v);
        ViewCompat.dispatchApplyWindowInsets(v, inset(63, 48, 0));
        ViewCompat.dispatchApplyWindowInsets(v, inset(63, 48, 0));
        assertEquals(4 + 63, v.getPaddingTop());
        assertEquals(14 + 48, v.getPaddingBottom());
    }
}
