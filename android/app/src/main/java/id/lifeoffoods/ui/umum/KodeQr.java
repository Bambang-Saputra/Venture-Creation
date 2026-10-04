package id.lifeoffoods.ui.umum;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.EnumMap;
import java.util.Map;

/** QR kode pickup K14 (Figma), dibuat di HP dengan ZXing tanpa jaringan. */
public final class KodeQr {

    private KodeQr() {}

    /** Bitmap persegi sisi {@code px}, modul berwarna {@code warna} di atas putih. */
    @Nullable
    public static Bitmap gambar(String isi, int px, @ColorInt int warna) {
        Map<EncodeHintType, Object> opsi = new EnumMap<>(EncodeHintType.class);
        opsi.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        opsi.put(EncodeHintType.MARGIN, 0);
        BitMatrix m;
        try {
            m = new QRCodeWriter().encode(isi, BarcodeFormat.QR_CODE, px, px, opsi);
        } catch (WriterException | IllegalArgumentException e) {
            return null;
        }
        int w = m.getWidth();
        int h = m.getHeight();
        int[] piksel = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                piksel[y * w + x] = m.get(x, y) ? warna : Color.WHITE;
            }
        }
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        b.setPixels(piksel, 0, w, 0, 0, w, h);
        return b;
    }
}
