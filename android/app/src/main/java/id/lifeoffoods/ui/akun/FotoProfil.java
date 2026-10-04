package id.lifeoffoods.ui.akun;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.MeResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Foto profil K19: foto dari galeri diperkecil di HP sebelum diunggah. Server menolak sisi lebih
 * dari 4096 px dan berkas lebih dari 5 MB, dan foto kamera HP sekarang sering lebih besar dari itu.
 */
final class FotoProfil {

    static final int SISI_MAKS = 1600;

    private FotoProfil() {}

    /** JPEG kualitas 85 dengan sisi terpanjang paling banyak 1600 px, atau null kalau gagal. */
    @Nullable
    static byte[] siapkan(ContentResolver cr, Uri uri) {
        Bitmap b;
        try {
            b =
                    Build.VERSION.SDK_INT >= 28
                            ? lewatImageDecoder(cr, uri)
                            : lewatBitmapFactory(cr, uri);
        } catch (IOException | RuntimeException e) {
            return null;
        }
        if (b == null) {
            return null;
        }
        float skala = Math.min(1f, SISI_MAKS / (float) Math.max(b.getWidth(), b.getHeight()));
        if (skala < 1f) {
            b =
                    Bitmap.createScaledBitmap(
                            b,
                            Math.round(b.getWidth() * skala),
                            Math.round(b.getHeight() * skala),
                            true);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 85, out);
        return out.toByteArray();
    }

    /** API 28+: ImageDecoder sudah meluruskan orientasi EXIF dan bisa langsung memperkecil. */
    @androidx.annotation.RequiresApi(28)
    private static Bitmap lewatImageDecoder(ContentResolver cr, Uri uri) throws IOException {
        return ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(cr, uri),
                (decoder, info, src) -> {
                    int sisi = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                    if (sisi > SISI_MAKS) {
                        float s = SISI_MAKS / (float) sisi;
                        decoder.setTargetSize(
                                Math.round(info.getSize().getWidth() * s),
                                Math.round(info.getSize().getHeight() * s));
                    }
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
    }

    /** API 27: baca ukuran dulu supaya foto besar tidak memenuhi memori. */
    @Nullable
    private static Bitmap lewatBitmapFactory(ContentResolver cr, Uri uri) throws IOException {
        BitmapFactory.Options ukur = new BitmapFactory.Options();
        ukur.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, ukur);
        }
        int sampel = 1;
        while (Math.max(ukur.outWidth, ukur.outHeight) / (sampel * 2) >= SISI_MAKS) {
            sampel *= 2;
        }
        BitmapFactory.Options baca = new BitmapFactory.Options();
        baca.inSampleSize = sampel;
        try (InputStream in = cr.openInputStream(uri)) {
            return BitmapFactory.decodeStream(in, null, baca);
        }
    }

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
