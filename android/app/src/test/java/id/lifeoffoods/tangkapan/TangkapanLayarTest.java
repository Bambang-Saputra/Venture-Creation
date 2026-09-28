package id.lifeoffoods.tangkapan;

import static org.junit.Assume.assumeTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.os.Looper;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.github.takahirom.roborazzi.RoborazziKt;
import com.github.takahirom.roborazzi.RoborazziOptions;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.masuk.VerifikasiOtpFragment;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        lepasPabrikViewModelLama();
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

    @Test
    public void k07Beranda() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        buka();
        tunggu();
        tangkap("K07");
    }

    /**
     * AndroidViewModelFactory menyimpan Application pertama di field statis, lalu memakainya untuk
     * setiap AndroidViewModel. Robolectric membuat Application baru per tes, jadi tanpa reset ini
     * ViewModel di tes berikutnya memanggil API lewat klien aplikasi lama, bukan MockWebServer tes
     * ini. Di perangkat hanya ada satu Application, jadi masalah ini khusus tes.
     */
    private static void lepasPabrikViewModelLama() {
        for (Field f : ViewModelProvider.AndroidViewModelFactory.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())
                    && f.getType() == ViewModelProvider.AndroidViewModelFactory.class) {
                try {
                    f.setAccessible(true);
                    f.set(null, null);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(
                            "Tidak bisa mereset AndroidViewModelFactory", e);
                }
            }
        }
    }

    @Test
    public void k10DetailTas() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        buka();
        Bundle args = new Bundle();
        args.putLong("listing_id", 31);
        navigasi(R.id.k10_detail_tas, args);
        tunggu();
        tangkap("K10");
    }

    @Test
    public void k11MenuSatuan() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        buka();
        Bundle args = new Bundle();
        args.putLong("listing_id", 41);
        navigasi(R.id.k11_detail_menu, args);
        tunggu();
        tangkap("K11");
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
     * Beri waktu respons MockWebServer sampai ke main looper. K05 dan K07 memanggil beberapa
     * endpoint berurutan, dan saat memori laptop sempit 3 detik pernah tidak cukup, jadi batasnya 5
     * detik.
     */
    private static void tunggu() {
        for (int i = 0; i < 100; i++) {
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
                                + "\"latitude\":-6.225300,\"longitude\":106.808700,"
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
            if (path.matches("/api/listings/\\d+.*")) {
                return json("{\"data\":" + detail(path.contains("/41") ? 41 : 31) + "}");
            }
            if (path.startsWith("/api/listings") && path.contains("store_id")) {
                return json(
                        "{\"data\":["
                                + menu(
                                        41,
                                        "Croissant mentega",
                                        12000,
                                        28000,
                                        3,
                                        alergenJson("susu", "Susu", "contains")
                                                + ","
                                                + alergenJson("gluten", "Gluten", "contains"))
                                + ","
                                + menu(
                                        42,
                                        "Danish keju",
                                        14000,
                                        32000,
                                        2,
                                        alergenJson("susu", "Susu", "contains")
                                                + ","
                                                + alergenJson("telur", "Telur", "contains")
                                                + ","
                                                + alergenJson("gluten", "Gluten", "contains"))
                                + ","
                                + menu(
                                        43,
                                        "Cinnamon roll",
                                        10000,
                                        25000,
                                        4,
                                        alergenJson("kacang_tanah", "Kacang tanah", "may_contain"))
                                + "],\"current_page\":1,\"last_page\":1}");
            }
            if (path.startsWith("/api/favorites")) {
                return json("{\"data\":[]}");
            }
            if (path.startsWith("/api/listings")) {
                // Dua kartu flash untuk "Tutup kurang dari satu jam", dua kartu tas untuk
                // "Terdekat dari kamu", sama dengan contoh Figma K07.
                if (path.contains("ends_within_minutes")) {
                    return json(
                            "{\"data\":["
                                    + listing(
                                            31,
                                            "surprise_bag",
                                            "Tas Pastry Sore",
                                            "Kopi Kalyan",
                                            18000,
                                            55000,
                                            48,
                                            null,
                                            "20:00",
                                            "21:00")
                                    + ","
                                    + listing(
                                            32,
                                            "menu_item",
                                            "Roti Hari Ini",
                                            "Bakerman Blok M",
                                            25000,
                                            78000,
                                            72,
                                            null,
                                            "20:30",
                                            "22:00")
                                    + "],\"current_page\":1,\"last_page\":1}");
                }
                return json(
                        "{\"data\":["
                                + listing(
                                        31,
                                        "surprise_bag",
                                        "Tas Pastry Sore",
                                        "Kopi Kalyan",
                                        18000,
                                        55000,
                                        48,
                                        0.38,
                                        "20:30",
                                        "21:00")
                                + ","
                                + listing(
                                        32,
                                        "menu_item",
                                        "Roti Hari Ini",
                                        "Bakerman Blok M",
                                        25000,
                                        78000,
                                        72,
                                        1.24,
                                        "20:30",
                                        "22:00")
                                + "],\"current_page\":1,\"last_page\":1}");
            }
            if (path.startsWith("/api/notifications")) {
                return json("{\"data\":[],\"unread_count\":3,\"current_page\":1,\"last_page\":1}");
            }
            return new MockResponse().setResponseCode(404).setBody("{\"message\":\"x\"}");
        }

        private static String alergenJson(String kode, String nama, String presence) {
            return "{\"code\":\""
                    + kode
                    + "\",\"name\":\""
                    + nama
                    + "\",\"presence\":\""
                    + presence
                    + "\"}";
        }

        private static String menu(
                long id, String judul, long harga, long normal, int stok, String alergen) {
            return "{\"id\":"
                    + id
                    + ",\"type\":\"menu_item\",\"title\":\""
                    + judul
                    + "\",\"photo_url\":null,\"price_rupiah\":"
                    + harga
                    + ",\"original_value_rupiah\":"
                    + normal
                    + ",\"qty_remaining\":"
                    + stok
                    + ",\"pickup_start\":\"2026-09-18T20:30:00+07:00\""
                    + ",\"pickup_end\":\"2026-09-18T21:00:00+07:00\",\"minutes_until_end\":95"
                    + ",\"halal_label\":\"self_claim\",\"allergens\":["
                    + alergen
                    + "],\"distance_km\":null"
                    + ",\"store\":{\"id\":5,\"name\":\"Kopi Kalyan\",\"category\":\"cafe\"}}";
        }

        /** Contoh Figma K10 (tas 31) dan K11 (menu 41), toko Kopi Kalyan kira-kira 380 m. */
        private static String detail(long id) {
            boolean tas = id == 31;
            return "{\"id\":"
                    + id
                    + ",\"type\":\""
                    + (tas ? "surprise_bag" : "menu_item")
                    + "\",\"title\":\""
                    + (tas ? "Tas Pastry Sore" : "Croissant mentega")
                    + "\",\"photo_url\":null,\"price_rupiah\":"
                    + (tas ? 18000 : 12000)
                    + ",\"original_value_rupiah\":"
                    + (tas ? 55000 : 28000)
                    + ",\"qty_remaining\":"
                    + (tas ? 2 : 3)
                    + ",\"pickup_start\":\"2026-09-18T20:30:00+07:00\""
                    + ",\"pickup_end\":\"2026-09-18T21:00:00+07:00\",\"minutes_until_end\":95"
                    + ",\"halal_label\":\"certified\",\"allergens\":["
                    + alergenJson("susu", "Susu", "contains")
                    + ","
                    + (tas ? alergenJson("telur", "Telur", "contains") + "," : "")
                    + alergenJson("gluten", "Gluten", "contains")
                    + (tas ? "," + alergenJson("kacang_tanah", "Kacang tanah", "may_contain") : "")
                    + "],\"distance_km\":null"
                    + ",\"description\":\"Empat sampai enam potong pastry. Isi pasti dipilih"
                    + " barista dari stok sore itu, jadi bisa berbeda setiap hari.\""
                    + ",\"content_hint\":\"4-6 pastry campur\",\"ingredients_text\":null"
                    + ",\"halal_certificate_no\":\"ID00410000123450825\",\"status\":\"active\""
                    + ",\"is_available\":true,\"items\":["
                    + (tas
                            ? "{\"label\":\"Croissant mentega\",\"qty\":null},"
                                    + "{\"label\":\"Danish keju\",\"qty\":null},"
                                    + "{\"label\":\"Cinnamon roll atau roti manis lain\",\"qty\":null}"
                            : "")
                    + "],\"store\":{\"id\":5,\"name\":\"Kopi Kalyan\",\"category\":\"cafe\""
                    + ",\"address\":\"Jl. Jend. Sudirman Kav 52, Lobi Utama, Jakarta Selatan\""
                    + ",\"latitude\":-6.2263,\"longitude\":106.8120"
                    + ",\"hours_today\":{\"open_time\":\"07:00:00\",\"close_time\":\"21:00:00\""
                    + ",\"is_closed\":0}}}";
        }

        private static String listing(
                long id,
                String tipe,
                String judul,
                String toko,
                long harga,
                long normal,
                int sisaMenit,
                Double km,
                String mulai,
                String akhir) {
            return "{\"id\":"
                    + id
                    + ",\"type\":\""
                    + tipe
                    + "\",\"title\":\""
                    + judul
                    + "\",\"photo_url\":null,\"price_rupiah\":"
                    + harga
                    + ",\"original_value_rupiah\":"
                    + normal
                    + ",\"qty_remaining\":3,\"pickup_start\":\"2026-09-18T"
                    + mulai
                    + ":00+07:00\",\"pickup_end\":\"2026-09-18T"
                    + akhir
                    + ":00+07:00\",\"minutes_until_end\":"
                    + sisaMenit
                    + ",\"halal_label\":\"self_claim\",\"allergens\":[],\"distance_km\":"
                    + km
                    + ",\"store\":{\"id\":"
                    + (id - 26)
                    + ",\"name\":\""
                    + toko
                    + "\",\"category\":\"cafe\"}}";
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
