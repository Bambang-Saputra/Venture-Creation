package id.lifeoffoods.tangkapan;

import static org.junit.Assume.assumeTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.os.Looper;
import androidx.navigation.fragment.NavHostFragment;
import com.github.takahirom.roborazzi.RoborazziKt;
import com.github.takahirom.roborazzi.RoborazziOptions;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.masuk.VerifikasiOtpFragment;
import java.io.IOException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * Tangkapan layar tiap layar untuk dibandingkan dengan PNG Figma (frame 402x874, dirender 2x).
 * Jalankan: {@code ./gradlew testDebugUnitTest -Ptangkapan --tests '*TangkapanLayarTest*'}. Hasil
 * PNG ada di {@code app/build/outputs/roborazzi/}. Tanpa -Ptangkapan, tes ini dilewati.
 *
 * <p>API diganti MockWebServer berisi data contoh Figma, jadi layar tampil dengan isi yang sama
 * dengan desain.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 36, qualifiers = "w402dp-h874dp-port-xhdpi")
public class TangkapanLayarTest {

    private MockWebServer server;
    private ActivityController<MainActivity> kontrol;

    @Before
    public void siapkan() throws IOException {
        assumeTrue(Boolean.getBoolean("lof.tangkapan"));
        server = new MockWebServer();
        server.setDispatcher(new DataContoh());
        server.start();
        app().arahkanApiKe(server.url("/api/").toString());
        app().sesi().hapus();
    }

    @After
    public void bereskan() throws IOException {
        if (kontrol != null) {
            kontrol.pause().stop().destroy();
        }
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void k01PilihPeran() {
        buka();
        tangkap("K01");
    }

    @Test
    public void k02Masuk() {
        buka().bukaAlur(SesiPengguna.PERAN_KONSUMEN);
        tangkap("K02");
    }

    @Test
    public void m01MasukMitra() {
        buka().bukaAlur(SesiPengguna.PERAN_MITRA);
        tangkap("M01");
    }

    @Test
    public void k03Verifikasi() {
        buka().bukaAlur(SesiPengguna.PERAN_KONSUMEN);
        Bundle args = new Bundle();
        args.putString(VerifikasiOtpFragment.ARG_NOMOR, "6281234567890");
        args.putString(VerifikasiOtpFragment.ARG_PERAN, SesiPengguna.PERAN_KONSUMEN);
        args.putInt(VerifikasiOtpFragment.ARG_JEDA, 42);
        args.putString(VerifikasiOtpFragment.ARG_KODE_UJI, "481244");
        navigasi(R.id.ke_verifikasi, args);
        tangkap("K03");
    }

    @Test
    public void k04LengkapiProfil() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, true);
        buka();
        tunggu();
        tangkap("K04");
    }

    @Test
    public void k04Galat() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, true);
        MainActivity a = buka();
        tunggu();
        ((android.widget.EditText) a.findViewById(R.id.nama)).setText("D");
        ((android.widget.EditText) a.findViewById(R.id.email)).setText("dara@email");
        a.findViewById(R.id.tombol_lanjut).performClick();
        idle();
        tangkap("K04_galat");
    }

    @Test
    public void k05Alergi() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, true);
        buka();
        navigasi(R.id.ke_alergi, null);
        tunggu();
        tangkap("K05");
    }

    private MainActivity buka() {
        kontrol = Robolectric.buildActivity(MainActivity.class).setup();
        idle();
        return kontrol.get();
    }

    private void navigasi(int aksi, Bundle args) {
        NavHostFragment host =
                (NavHostFragment)
                        kontrol.get().getSupportFragmentManager().findFragmentById(R.id.nav_host);
        host.getNavController().navigate(aksi, args);
        idle();
    }

    /**
     * Beri waktu respons MockWebServer sampai ke main looper. K05 memanggil dua endpoint berurutan,
     * jadi batasnya dibuat longgar (3 detik).
     */
    private static void tunggu() {
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            idle();
        }
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void tangkap(String nama) {
        idle();
        // captureScreenRoboImage, bukan captureRoboImage: overload captureRoboImage ikut menyebut
        // kelas Compose yang tidak ada di proyek ini, jadi javac menolaknya.
        RoborazziKt.captureScreenRoboImage(nama + ".png", new RoborazziOptions());
    }

    private static LofApp app() {
        return (LofApp) org.robolectric.RuntimeEnvironment.getApplication();
    }

    /** Jawaban API dengan isi yang sama dengan contoh di Figma. */
    private static final class DataContoh extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest r) {
            String path = r.getPath() == null ? "" : r.getPath();
            if (path.startsWith("/api/me") && "GET".equals(r.getMethod())) {
                return json(
                        "{\"user\":{\"id\":12,\"name\":\"Dara Renata\","
                                + "\"email\":\"dara.renata@email.com\",\"phone\":\"6281234567890\","
                                + "\"role\":\"consumer\",\"has_google\":false},"
                                + "\"is_profile_complete\":false,"
                                + "\"consumer_profile\":{\"area_label\":\"SCBD, Jakarta Selatan\","
                                + "\"latitude\":null,\"longitude\":null,"
                                + "\"notify_favorite_store\":true,\"notify_pickup_reminder\":true,"
                                + "\"notify_promo\":false},"
                                + "\"allergens\":[{\"code\":\"kacang_tanah\",\"name\":\"Kacang tanah\","
                                + "\"type\":\"allergen\",\"severity\":\"avoid\"}]}");
            }
            if (path.startsWith("/api/allergens")) {
                return json(
                        "{\"data\":["
                                + alergen("kacang_tanah", "Kacang tanah", "allergen")
                                + ","
                                + alergen("kacang_pohon", "Kacang pohon (almond, mete)", "allergen")
                                + ","
                                + alergen("susu", "Susu dan produk susu", "allergen")
                                + ","
                                + alergen("telur", "Telur", "allergen")
                                + ","
                                + alergen("gluten", "Gandum dan gluten", "allergen")
                                + ","
                                + alergen("kedelai", "Kedelai", "allergen")
                                + ","
                                + alergen("ikan", "Ikan", "allergen")
                                + ","
                                + alergen("udang_kerang", "Udang, kepiting, dan kerang", "allergen")
                                + ","
                                + alergen("wijen", "Wijen", "allergen")
                                + ","
                                + alergen("vegetarian", "Vegetarian", "diet")
                                + ","
                                + alergen("vegan", "Vegan", "diet")
                                + ","
                                + alergen("tanpa_babi", "Tanpa babi", "diet")
                                + ","
                                + alergen("tanpa_alkohol", "Tanpa alkohol", "diet")
                                + "]}");
            }
            return new MockResponse().setResponseCode(404).setBody("{\"message\":\"x\"}");
        }

        private static String alergen(String kode, String nama, String tipe) {
            return "{\"code\":\""
                    + kode
                    + "\",\"name\":\""
                    + nama
                    + "\",\"type\":\""
                    + tipe
                    + "\"}";
        }

        private static MockResponse json(String body) {
            return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
        }
    }
}
