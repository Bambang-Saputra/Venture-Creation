package id.lifeoffoods;

import android.app.Application;
import androidx.annotation.VisibleForTesting;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.data.api.ApiClient;
import id.lifeoffoods.data.api.LofApi;

/** Titik awal aplikasi. Menyimpan satu instance sesi dan klien API untuk semua layar. */
public class LofApp extends Application {

    private SesiPengguna sesi;
    private LofApi api;

    @Override
    public void onCreate() {
        super.onCreate();
        sesi = new SesiPengguna(this);
        api = ApiClient.buat(BuildConfig.API_BASE_URL, sesi, BuildConfig.DEBUG);
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
