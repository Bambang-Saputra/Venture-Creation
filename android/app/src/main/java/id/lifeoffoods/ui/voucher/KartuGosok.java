package id.lifeoffoods.ui.voucher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import id.lifeoffoods.R;

/**
 * Lapisan perak yang bisa digosok dengan jari. Hadiah ditaruh di view di bawahnya; setelah sekitar
 * setengah lapisan terhapus, sisanya dibuka sendiri dan {@link Selesai} dipanggil.
 */
public class KartuGosok extends View {

    public interface Selesai {
        void terbuka();
    }

    private static final float AMBANG = 0.5f;

    private final Paint penghapus = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint teks = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path jejak = new Path();
    @Nullable private Bitmap lapisan;
    @Nullable private Canvas kanvas;
    @Nullable private Selesai selesai;
    private boolean terbuka;
    private boolean aktif = true;

    public KartuGosok(Context c, @Nullable AttributeSet a) {
        super(c, a);
        penghapus.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        penghapus.setStyle(Paint.Style.STROKE);
        penghapus.setStrokeCap(Paint.Cap.ROUND);
        penghapus.setStrokeJoin(Paint.Join.ROUND);
        penghapus.setStrokeWidth(36 * getResources().getDisplayMetrics().density);
        teks.setColor(0xFF4E5F55);
        teks.setTextAlign(Paint.Align.CENTER);
        teks.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        teks.setFakeBoldText(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void saatTerbuka(Selesai s) {
        selesai = s;
    }

    /** Matikan gosokan sampai hasil dari server siap. */
    public void setAktif(boolean a) {
        aktif = a;
    }

    @Override
    protected void onSizeChanged(int w, int h, int wLama, int hLama) {
        super.onSizeChanged(w, h, wLama, hLama);
        if (w <= 0 || h <= 0) {
            return;
        }
        lapisan = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        kanvas = new Canvas(lapisan);
        Paint perak = new Paint();
        perak.setShader(
                new LinearGradient(
                        0,
                        0,
                        w,
                        h,
                        new int[] {0xFFE3E8E4, 0xFFC7D0CA, 0xFFEFF3F0},
                        null,
                        Shader.TileMode.CLAMP));
        kanvas.drawRect(0, 0, w, h, perak);
        teks.setColor(ContextCompat.getColor(getContext(), R.color.teks_kuat));
        kanvas.drawText(
                getContext().getString(R.string.gosok_petunjuk),
                w / 2f,
                h / 2f - (teks.descent() + teks.ascent()) / 2,
                teks);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (!terbuka && lapisan != null) {
            c.drawBitmap(lapisan, 0, 0, null);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (terbuka || !aktif || kanvas == null) {
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                jejak.reset();
                jejak.moveTo(e.getX(), e.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                jejak.lineTo(e.getX(), e.getY());
                kanvas.drawPath(jejak, penghapus);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (bagianTerhapus() >= AMBANG) {
                    buka();
                }
                break;
            default:
                break;
        }
        return true;
    }

    /** Buka seluruh lapisan, misalnya lewat tombol "Buka langsung" untuk aksesibilitas. */
    public void buka() {
        if (terbuka) {
            return;
        }
        terbuka = true;
        invalidate();
        if (selesai != null) {
            selesai.terbuka();
        }
    }

    /** Perkiraan bagian lapisan yang sudah terhapus, dari sampel piksel berjarak. */
    private float bagianTerhapus() {
        if (lapisan == null) {
            return 0;
        }
        int langkah = Math.max(4, lapisan.getWidth() / 40);
        int total = 0;
        int bening = 0;
        for (int x = 0; x < lapisan.getWidth(); x += langkah) {
            for (int y = 0; y < lapisan.getHeight(); y += langkah) {
                total++;
                if ((lapisan.getPixel(x, y) >>> 24) == 0) {
                    bening++;
                }
            }
        }
        return total == 0 ? 0 : (float) bening / total;
    }
}
