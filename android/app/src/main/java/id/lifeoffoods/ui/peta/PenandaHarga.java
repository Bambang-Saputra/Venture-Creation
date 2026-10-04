package id.lifeoffoods.ui.peta;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import androidx.core.content.ContextCompat;
import id.lifeoffoods.R;
import id.lifeoffoods.databinding.PenandaHargaBinding;

/**
 * Menggambar penanda harga K08 ke bitmap untuk ikon Marker osmdroid. Terpilih: pil hijau merek
 * dengan teks putih (Figma "Rp18.000"); lainnya pil putih dengan teks gelap.
 */
final class PenandaHarga {

    private PenandaHarga() {}

    static BitmapDrawable gambar(Context c, CharSequence teks, boolean terpilih) {
        PenandaHargaBinding b = PenandaHargaBinding.inflate(LayoutInflater.from(c));
        b.harga.setText(teks);
        if (terpilih) {
            b.harga.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(c, R.color.merek_utama)));
            b.harga.setTextColor(ContextCompat.getColor(c, R.color.latar_kartu));
        } else {
            b.harga.setTextColor(ContextCompat.getColor(c, R.color.teks_utama));
        }
        View v = b.getRoot();
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(spec, spec);
        int w = Math.max(1, v.getMeasuredWidth());
        int h = Math.max(1, v.getMeasuredHeight());
        v.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(bmp));
        return new BitmapDrawable(c.getResources(), bmp);
    }
}
