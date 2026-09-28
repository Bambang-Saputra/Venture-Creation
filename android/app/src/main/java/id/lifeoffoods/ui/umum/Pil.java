package id.lifeoffoods.ui.umum;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import id.lifeoffoods.R;

/**
 * Komponen "Chip" Figma: pil kecil 10.5 bold dengan ikon opsional 11dp. Dibuat lewat kode karena
 * isinya (label halal, alergen, jam ambil) berasal dari API.
 */
public final class Pil {

    private Pil() {}

    public static TextView buat(
            Context c,
            CharSequence teks,
            @DrawableRes int latar,
            @ColorRes int warna,
            @DrawableRes int ikon) {
        TextView tv = new TextView(c, null, 0, R.style.Teks_Pil);
        tv.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setText(teks);
        tv.setBackgroundResource(latar);
        tv.setTextColor(ContextCompat.getColor(c, warna));
        ikon(tv, ikon, warna);
        return tv;
    }

    /** Ikon 11dp di depan teks, diwarnai sama dengan teks. 0 berarti tanpa ikon. */
    public static void ikon(TextView tv, @DrawableRes int ikon, @ColorRes int warna) {
        if (ikon == 0) {
            tv.setCompoundDrawablesRelative(null, null, null, null);
            return;
        }
        @Nullable Drawable d = AppCompatResources.getDrawable(tv.getContext(), ikon);
        if (d == null) {
            return;
        }
        d = DrawableCompat.wrap(d.mutate());
        DrawableCompat.setTint(d, ContextCompat.getColor(tv.getContext(), warna));
        int ukuran = Math.round(11 * tv.getResources().getDisplayMetrics().density);
        d.setBounds(0, 0, ukuran, ukuran);
        tv.setCompoundDrawablesRelative(d, null, null, null);
    }
}
