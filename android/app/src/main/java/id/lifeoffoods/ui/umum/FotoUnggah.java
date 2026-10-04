package id.lifeoffoods.ui.umum;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Foto dari galeri untuk diunggah (F-20): profil K19, toko M14, tas M09, jualan M10. Diperkecil di
 * HP karena server menolak sisi lebih dari 4096 px dan berkas lebih dari 5 MB, sedangkan foto
 * kamera HP sekarang sering lebih besar dari itu.
 */
public final class FotoUnggah {

    public static final int SISI_MAKS = 1600;

    public interface Hasil {
        /**
         * Di thread utama; null kalau foto tidak bisa dibaca. Tidak dipanggil kalau layar hilang.
         */
        void terima(@Nullable byte[] jpeg);
    }

    private FotoUnggah() {}

    /** Field multipart "photo" sesuai kontrak API bagian unggah foto. */
    public static MultipartBody.Part bagian(@NonNull byte[] jpeg, @NonNull String namaBerkas) {
        RequestBody isi = RequestBody.create(jpeg, MediaType.get("image/jpeg"));
        return MultipartBody.Part.createFormData("photo", namaBerkas, isi);
    }

    /** Membaca dan memperkecil foto bisa ratusan milidetik, jadi dikerjakan di thread lain. */
    public static void baca(Fragment f, Uri uri, Hasil hasil) {
        ContentResolver cr = f.requireContext().getContentResolver();
        new Thread(
                        () -> {
                            byte[] jpeg = siapkan(cr, uri);
                            if (f.getActivity() == null) {
                                return;
                            }
                            f.requireActivity()
                                    .runOnUiThread(
                                            () -> {
                                                if (f.isAdded() && f.getView() != null) {
                                                    hasil.terima(jpeg);
                                                }
                                            });
                        },
                        "foto-unggah")
                .start();
    }

    /** JPEG kualitas 85 dengan sisi terpanjang paling banyak 1600 px, atau null kalau gagal. */
    @Nullable
    public static byte[] siapkan(ContentResolver cr, Uri uri) {
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

    /** Pratinjau dari hasil siapkan(), untuk kotak foto sebelum diunggah. */
    @Nullable
    public static Bitmap pratinjau(@Nullable byte[] jpeg) {
        return jpeg == null ? null : BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
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
}
