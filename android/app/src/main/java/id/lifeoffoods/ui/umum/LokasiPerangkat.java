package id.lifeoffoods.ui.umum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Posisi HP untuk K08, lewat LocationManager bawaan (tanpa Google Play Services). Izin diminta di
 * K06; kalau ditolak, layar memakai koordinat area profil seperti sebelumnya.
 */
public final class LokasiPerangkat {

    /** Posisi yang lebih tua dari ini dianggap basi dan diminta yang baru. */
    private static final long BATAS_UMUR_MS = 10 * 60 * 1000L;

    private static final long BATAS_TUNGGU_MS = 8000L;

    public interface Hasil {
        /** Selalu dipanggil sekali di thread utama; null kalau posisi tidak didapat. */
        void terima(@Nullable Location lokasi);
    }

    private LokasiPerangkat() {}

    public static final String[] IZIN = {
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
    };

    /** Lokasi perkiraan sudah cukup: jarak ditampilkan per 100 m. */
    public static boolean diizinkan(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission") // diperiksa oleh diizinkan() di awal
    public static void ambil(Context c, Hasil hasil) {
        Handler utama = new Handler(Looper.getMainLooper());
        LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null || !diizinkan(c)) {
            utama.post(() -> hasil.terima(null));
            return;
        }
        Location terbaru = null;
        for (String p : lm.getProviders(true)) {
            Location l = lm.getLastKnownLocation(p);
            if (l != null && (terbaru == null || l.getTime() > terbaru.getTime())) {
                terbaru = l;
            }
        }
        if (terbaru != null && System.currentTimeMillis() - terbaru.getTime() < BATAS_UMUR_MS) {
            Location segar = terbaru;
            utama.post(() -> hasil.terima(segar));
            return;
        }

        String penyedia =
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                        ? LocationManager.NETWORK_PROVIDER
                        : lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                                ? LocationManager.GPS_PROVIDER
                                : null;
        Location cadangan = terbaru;
        if (penyedia == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // API 27-29 tidak punya getCurrentLocation; posisi lama lebih baik daripada tidak ada.
            utama.post(() -> hasil.terima(cadangan));
            return;
        }
        AtomicBoolean selesai = new AtomicBoolean(false);
        CancellationSignal batal = new CancellationSignal();
        lm.getCurrentLocation(
                penyedia,
                batal,
                ContextCompat.getMainExecutor(c),
                l -> {
                    if (selesai.compareAndSet(false, true)) {
                        hasil.terima(l != null ? l : cadangan);
                    }
                });
        utama.postDelayed(
                () -> {
                    if (selesai.compareAndSet(false, true)) {
                        batal.cancel();
                        hasil.terima(cadangan);
                    }
                },
                BATAS_TUNGGU_MS);
    }
}
