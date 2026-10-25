package id.lifeoffoods.ui.umum;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

/**
 * Bagikan teks lewat WhatsApp, atau lewat pilihan aplikasi kalau WhatsApp tidak terpasang. Dipakai
 * undangan kasir (M15) dan bagikan jualan (K10, K11).
 */
public final class BagikanWhatsapp {

    private static final String PAKET_WHATSAPP = "com.whatsapp";

    private BagikanWhatsapp() {}

    /**
     * @param judulPilihan judul daftar aplikasi yang muncul kalau WhatsApp tidak ada.
     */
    public static void kirim(Fragment f, String teks, @StringRes int judulPilihan) {
        Intent kirim =
                new Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, teks);
        try {
            f.startActivity(new Intent(kirim).setPackage(PAKET_WHATSAPP));
        } catch (ActivityNotFoundException e) {
            f.startActivity(Intent.createChooser(kirim, f.getString(judulPilihan)));
        }
    }
}
