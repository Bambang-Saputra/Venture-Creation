package id.lifeoffoods;

import android.app.Application;
import androidx.annotation.VisibleForTesting;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.data.api.ApiClient;
import id.lifeoffoods.data.api.LofApi;
import id.lifeoffoods.notifikasi.CekNotifikasiWorker;
import id.lifeoffoods.notifikasi.NotifikasiSistem;
import id.lifeoffoods.notifikasi.PemantauNotifikasi;

/** Titik awal aplikasi. Menyimpan satu instance sesi dan klien API untuk semua layar. */
public class LofApp extends Application {

    private SesiPengguna sesi;
    private LofApi api;

    @Override
    public void onCreate() {
        super.onCreate();
        sesi = new SesiPengguna(this);
        api = ApiClient.buat(BuildConfig.API_BASE_URL, sesi, BuildConfig.DEBUG);

        // Notifikasi tanpa Firebase: pop up saat aplikasi terbuka, dan pekerja latar tiap
        // sekitar 15 menit saat tertutup.
        NotifikasiSistem.buatSaluran(this);
        registerActivityLifecycleCallbacks(new PemantauNotifikasi(this));
        try {
            CekNotifikasiWorker.jadwalkan(this);
        } catch (IllegalStateException e) {
            // WorkManager belum siap (misalnya di uji Robolectric); dicoba lagi saat app dibuka.
        }
    }

    public SesiPengguna sesi() {
        return sesi;
    }

    public LofApi api() {
        return api;
    }

    /** Hanya untuk tes: arahkan API ke server tiruan (MockWebServer). */
    @VisibleForTesting
    public void arahkanApiKe(String baseUrl) {
        api = ApiClient.buat(baseUrl, sesi, false);
    }
}
