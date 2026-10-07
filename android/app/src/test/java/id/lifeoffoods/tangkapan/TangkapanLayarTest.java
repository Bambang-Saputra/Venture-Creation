package id.lifeoffoods.tangkapan;

import static org.junit.Assume.assumeTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.github.takahirom.roborazzi.RoborazziKt;
import com.github.takahirom.roborazzi.RoborazziOptions;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.filter.FilterBundle;
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

    /** GET /favorites mengirim contoh Figma K16; tetap kosong untuk layar lain (hati K10). */
    private static volatile boolean isiFavorit;

    /**
     * Akun mitra tanpa toko (M03/M04): null = toko sudah ada (bawaan), lainnya isi GET
     * /partner/application: "kosong" (belum mendaftar), "menunggu", atau "ditolak".
     */
    private static volatile String pendaftaran;

    @Before
    public void siapkan() throws IOException {
        assumeTrue(Boolean.getBoolean("lof.tangkapan"));
        isiFavorit = false;
        pendaftaran = null;
        lepasPabrikViewModelLama();
        server = new MockWebServer();
        server.setDispatcher(new DataContoh());
        server.start();
        app().arahkanApiKe(server.url("/api/").toString());
        app().sesi().hapus();
        // Default: K06 sudah dilihat, supaya buka() tanpa sesi berhenti di K01.
        app().sesi().tandaiPerkenalan(true);
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

    /** K09 dengan pilihan yang sama dengan Figma 58:1137. */
    @Test
    public void k09Filter() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        buka();
        tunggu();
        FilterJualan f = new FilterJualan();
        f.alergen.put("kacang_tanah", "Kacang tanah");
        f.halal = true;
        f.radiusKm = 1;
        f.jam = FilterJualan.JAM_20_22;
        navigasi(R.id.k09_filter, FilterBundle.ke(f));
        tunggu();
        tangkapPanjang("K09");
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

    @Test
    public void k12RingkasanTas() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        buka();
        Bundle args = new Bundle();
        args.putLongArray("id_jualan", new long[] {31});
        args.putIntArray("jumlah", new int[] {1});
        navigasi(R.id.k12_ringkasan_tas, args);
        tunggu();
        tangkap("K12");
    }

    @Test
    public void k13RingkasanMenu() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        buka();
        Bundle args = new Bundle();
        args.putLongArray("id_jualan", new long[] {41, 42});
        args.putIntArray("jumlah", new int[] {1, 1});
        args.putIntArray("stok", new int[] {3, 2});
        args.putLongArray("harga_normal", new long[] {28000, 27000});
        navigasi(R.id.k13_ringkasan_menu, args);
        tunggu();
        tangkap("K13");
    }

    @Test
    public void k14KodePickup() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        buka();
        Bundle args = new Bundle();
        args.putLong("id_pesanan", 88);
        navigasi(R.id.k14_kode_pickup, args);
        tunggu();
        tangkap("K14");
    }

    @Test
    public void k15PesananSaya() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        buka();
        navigasi(R.id.k15_pesanan, null);
        tunggu();
        tangkap("K15");
    }

    @Test
    public void k15Riwayat() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        MainActivity a = buka();
        navigasi(R.id.k15_pesanan, null);
        tunggu();
        a.findViewById(R.id.tab_riwayat).performClick();
        tunggu();
        tangkap("K15-riwayat");
    }

    @Test
    public void k15Ulasan() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        MainActivity a = buka();
        navigasi(R.id.k15_pesanan, null);
        tunggu();
        a.findViewById(R.id.tab_riwayat).performClick();
        tunggu();
        a.findViewById(R.id.ulasan).performClick();
        tunggu();
        tangkap("K15-ulasan");
    }

    @Test
    public void m11PesananMasuk() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m11_pesanan, null);
        tunggu();
        tangkap("M11");
    }

    /** Mitra yang sudah masuk dibuka di M05. Contoh Figma: belum mencatat, tutup 2 jam lagi. */
    @Test
    public void m05DashboardMitra() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        tangkapPanjang("M05");
    }

    @Test
    public void m12CocokkanKodeBerhasil() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        navigasi(R.id.m12_cocokkan, null);
        // Huruf kecil diterima dan dikirim kapital; karakter keenam langsung menukar kode.
        ((android.widget.EditText) a.findViewById(R.id.kode)).setText("lf7q2k");
        tunggu();
        tangkap("M12");
    }

    @Test
    public void m12KodeSudahDipakai() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        navigasi(R.id.m12_cocokkan, null);
        ((android.widget.EditText) a.findViewById(R.id.kode)).setText("M3K8PD");
        tunggu();
        tangkap("M12-galat");
    }

    @Test
    public void m10KelolaJualan() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m10_kelola, null);
        tunggu();
        tangkap("M10");
    }

    @Test
    public void m17KelolaMenu() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        Bundle args = new Bundle();
        args.putString("tipe", "menu_item");
        navigasi(R.id.m10_kelola, args);
        tunggu();
        tangkap("M17");
    }

    @Test
    public void m09PasangTas() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m09_pasang_tas, null);
        tunggu();
        tangkapPanjang("M09");
    }

    @Test
    public void m09GalatAlergen() {
        // Terbitkan tanpa menyatakan alergen: pesan kriteria 10 tampil di bawah chip.
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        navigasi(R.id.m09_pasang_tas, null);
        tunggu();
        a.findViewById(R.id.tombol_terbit).performClick();
        idle();
        tangkapPanjang("M09-galat");
    }

    @Test
    public void m16PasangMenu() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        navigasi(R.id.m16_pasang_menu, null);
        tunggu();
        // Isi stok dan harga item pertama, seperti contoh Figma.
        android.view.ViewGroup daftar = a.findViewById(R.id.daftar_produk);
        View pertama = daftar.getChildAt(0);
        for (int i = 0; i < 4; i++) {
            pertama.findViewById(R.id.tambah).performClick();
        }
        ((android.widget.EditText) pertama.findViewById(R.id.harga)).setText("9000");
        idle();
        tangkapPanjang("M16");
    }

    @Test
    public void m06CatatSisa() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m06_catat, null);
        tunggu();
        tangkapPanjang("M06");
    }

    /** "Atur tujuan" diketuk: pil tujuan muncul di tiap produk (PRD-12 alur 3). */
    @Test
    public void m06AturTujuan() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        navigasi(R.id.m06_catat, null);
        tunggu();
        a.findViewById(R.id.tombol_atur_tujuan).performClick();
        idle();
        tangkapPanjang("M06-tujuan");
    }

    @Test
    public void m19CatatSisaTimbang() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        navigasi(R.id.m06_catat, null);
        tunggu();
        a.findViewById(R.id.tab_menu).performClick();
        idle();
        tangkap("M19");
    }

    @Test
    public void m07LaporanMingguan() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m07_laporan, null);
        tunggu();
        tangkapPanjang("M07");
    }

    /** Minggu lalu: tiga hari tidak dicatat tampil sebagai celah, bukan nol (PRD-13 kriteria 5). */
    @Test
    public void m07LaporanMingguLaluBerCelah() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        navigasi(R.id.m07_laporan, null);
        tunggu();
        a.findViewById(R.id.tombol_sebelumnya).performClick();
        tunggu();
        tangkapPanjang("M07-celah");
    }

    /** M14 dari tab Toko; contoh Figma. */
    @Test
    public void m14ProfilToko() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m14_toko, null);
        tunggu();
        tangkapPanjang("M14");
    }

    @Test
    public void m15PengaturanToko() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m15_pengaturan, null);
        tunggu();
        tangkap("M15");
    }

    @Test
    public void k06Onboarding() {
        // Instalasi baru tanpa sesi: K06 tampil sebelum K01.
        app().sesi().tandaiPerkenalan(false);
        buka();
        tangkap("K06");
    }

    @Test
    public void k08Peta() {
        bukaKonsumen(R.id.k08_peta);
        tangkap("K08");
    }

    @Test
    public void k06HalamanTerakhir() {
        app().sesi().tandaiPerkenalan(false);
        MainActivity a = buka();
        ((androidx.viewpager2.widget.ViewPager2) a.findViewById(R.id.halaman))
                .setCurrentItem(2, false);
        tunggu();
        tangkap("K06-akhir");

        // Tombol terakhir membuka K01 dan K06 tidak tampil lagi, termasuk setelah logout.
        a.findViewById(R.id.tombol_lanjut).performClick();
        idle();
        org.junit.Assert.assertNotNull(a.findViewById(R.id.kartu_konsumen));
        org.junit.Assert.assertTrue(app().sesi().perkenalanSelesai());
        app().sesi().hapus();
        org.junit.Assert.assertTrue(app().sesi().perkenalanSelesai());
    }

    @Test
    public void k08Daftar() {
        MainActivity a = bukaKonsumen(R.id.k08_peta);
        a.findViewById(R.id.tab_daftar).performClick();
        tunggu();
        tangkap("K08-daftar");
    }

    @Test
    public void k16Favorit() {
        isiFavorit = true;
        bukaKonsumen(R.id.k16_favorit);
        tangkap("K16");
    }

    @Test
    public void k17Notifikasi() {
        bukaKonsumen(R.id.k17_notifikasi);
        tangkap("K17");
    }

    @Test
    public void k18Profil() {
        bukaKonsumen(R.id.k18_profil);
        tangkap("K18");
    }

    @Test
    public void k19EditProfil() {
        bukaKonsumen(R.id.k19_edit_profil);
        tangkap("K19");
    }

    @Test
    public void k20Pengaturan() {
        bukaKonsumen(R.id.k20_pengaturan);
        tangkap("K20");
    }

    private MainActivity bukaKonsumen(int tujuan) {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_KONSUMEN, false);
        MainActivity a = buka();
        tunggu();
        navigasi(tujuan, null);
        tunggu();
        return a;
    }

    @Test
    public void m21RiwayatPesanan() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        navigasi(R.id.m11_pesanan, null);
        tunggu();
        a.findViewById(R.id.tab_riwayat).performClick();
        tunggu();
        tangkap("M21");
    }

    @Test
    public void m08SaranProduksi() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m08_saran, null);
        tunggu();
        tangkapPanjang("M08");
    }

    @Test
    public void m13Saldo() {
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        navigasi(R.id.m13_saldo, null);
        tunggu();
        tangkap("M13");
    }

    /** Mitra baru tanpa toko: M05 langsung berganti ke M03, langkah 1. */
    @Test
    public void m03DaftarMitra() {
        pendaftaran = "kosong";
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        isiDataUsaha(a);
        tangkap("M03");
    }

    @Test
    public void m03GalatKosong() {
        pendaftaran = "kosong";
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        a.findViewById(R.id.tombol_utama).performClick();
        idle();
        tangkap("M03-galat");
    }

    @Test
    public void m04Verifikasi() {
        pendaftaran = "kosong";
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        MainActivity a = buka();
        tunggu();
        isiDataUsaha(a);
        a.findViewById(R.id.tombol_utama).performClick();
        idle();
        ((android.widget.EditText) a.findViewById(R.id.pemilik)).setText("Kalyan Pratama");
        idle();
        tangkap("M04");
    }

    @Test
    public void m04Menunggu() {
        pendaftaran = "menunggu";
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        tangkap("M04-menunggu");
    }

    @Test
    public void m03Ditolak() {
        pendaftaran = "ditolak";
        app().sesi().simpan("token-uji", SesiPengguna.PERAN_MITRA, false);
        buka();
        tunggu();
        tangkap("M03-ditolak");
    }

    /** Isi M03 seperti contoh Figma 65:1754. */
    private static void isiDataUsaha(MainActivity a) {
        ((android.widget.EditText) a.findViewById(R.id.nama_usaha)).setText("Kopi Kalyan SCBD");
        ((android.widget.EditText) a.findViewById(R.id.alamat))
                .setText("Jl. Jend. Sudirman Kav 52, Lobi Utama");
        com.google.android.material.chip.ChipGroup grup = a.findViewById(R.id.grup_kategori);
        ((com.google.android.material.chip.Chip) grup.getChildAt(0)).setChecked(true);
        idle();
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

    /** Layar formulir yang panjang: bagian atas, lalu setelah digulir ke bawah ({nama}-bawah). */
    private void tangkapPanjang(String nama) {
        tangkap(nama);
        View gulir = kontrol.get().findViewById(R.id.gulir);
        if (gulir instanceof androidx.core.widget.NestedScrollView) {
            ((androidx.core.widget.NestedScrollView) gulir).fullScroll(View.FOCUS_DOWN);
            idle();
            tangkap(nama + "-bawah");
        }
    }

    private static LofApp app() {
        return (LofApp) org.robolectric.RuntimeEnvironment.getApplication();
    }

    /** GET /partner/application sesuai {@link #pendaftaran}. */
    private static String pendaftaranM03() {
        if ("kosong".equals(pendaftaran)) {
            return "{\"data\":null}";
        }
        boolean ditolak = "ditolak".equals(pendaftaran);
        return "{\"data\":{\"id\":3,\"status\":\""
                + (ditolak ? "rejected" : "pending")
                + "\",\"owner_name\":\"Kalyan Pratama\",\"store_name\":\"Kopi Kalyan SCBD\","
                + "\"category\":\"cafe\",\"address\":\"Jl. Jend. Sudirman Kav 52, Lobi Utama\","
                + "\"latitude\":null,\"longitude\":null,\"open_time\":\"07:00\","
                + "\"close_time\":\"21:00\",\"nib\":"
                + (ditolak ? "null" : "\"9120001234567\"")
                + ",\"halal_certificate_no\":null,\"rejection_reason\":"
                + (ditolak ? "\"Alamat belum lengkap, tambahkan nomor gedung.\"" : "null")
                + ",\"store_id\":null,\"submitted_at\":\"2026-10-07T10:15:00+07:00\"}}";
    }

    /** Jawaban API dengan isi yang sama dengan contoh di Figma. */
    private static final class DataContoh extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest r) {
            String path = r.getPath() == null ? "" : r.getPath();
            if (pendaftaran != null && path.equals("/api/partner/stores")) {
                return json("{\"data\":[]}");
            }
            if (path.equals("/api/partner/application")) {
                return json(pendaftaranM03());
            }
            if (path.startsWith("/api/me/impact")) {
                return json(
                        "{\"data\":{\"portions_rescued\":16,\"saved_rupiah\":412000,"
                                + "\"orders_completed\":14,"
                                + "\"member_since\":\"2026-03-05T10:00:00+07:00\"}}");
            }
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
                return json(isiFavorit ? favoritK16() : "{\"data\":[]}");
            }
            if (path.startsWith("/api/listings")) {
                // K09 menghitung jumlah dengan per_page=1; Figma menampilkan "Tampilkan 12 jualan".
                if (path.contains("per_page=1&") || path.endsWith("per_page=1")) {
                    return json("{\"data\":[],\"total\":12}");
                }
                // "Populer hari ini": urut dari jumlah dipesan, bukan iklan.
                if (path.contains("sort=popular")) {
                    return json(
                            "{\"data\":["
                                    + listing(
                                                    33,
                                                    "surprise_bag",
                                                    "Tas Kejutan Rotiku",
                                                    "Rotiku Palmerah",
                                                    15000,
                                                    45000,
                                                    180,
                                                    null,
                                                    "18:00",
                                                    "23:00")
                                            .replace(
                                                    "\"qty_remaining\":3",
                                                    "\"qty_remaining\":1,\"qty_ordered\":4")
                                    + ","
                                    + listing(
                                                    34,
                                                    "menu_item",
                                                    "Nasi Goreng Spesial",
                                                    "Nasi Goreng Pak Syahdan",
                                                    12000,
                                                    24000,
                                                    180,
                                                    null,
                                                    "18:00",
                                                    "23:00")
                                            .replace(
                                                    "\"qty_remaining\":3",
                                                    "\"qty_remaining\":2,\"qty_ordered\":2")
                                    + "],\"current_page\":1,\"last_page\":1}");
                }
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
            if (path.startsWith("/api/notifications") && path.contains("page=")) {
                return json(notifikasiK17());
            }
            if (path.startsWith("/api/notifications")) {
                return json("{\"data\":[],\"unread_count\":3,\"current_page\":1,\"last_page\":1}");
            }
            if (path.startsWith("/api/orders/preview")) {
                return json("{\"data\":" + pratinjau(r.getBody().readUtf8()) + "}");
            }
            if (path.startsWith("/api/partner/stores/5/reports/weekly")) {
                java.time.LocalDate senin =
                        id.lifeoffoods.data.LaporanMingguan.senin(
                                java.time.LocalDate.now(java.time.ZoneOffset.ofHours(7)));
                boolean ini = path.contains("week_start=" + senin);
                return json("{\"data\":" + laporan(ini ? senin : senin.minusWeeks(1), ini) + "}");
            }
            if (path.startsWith("/api/partner/stores/5/waste-logs")) {
                String hari = java.time.LocalDate.now(java.time.ZoneOffset.ofHours(7)).toString();
                // Contoh Figma M06: 6 croissant, 4 danish, 2 cinnamon, 3 roti, 5 kopi; sudah
                // dicatat.
                return json(
                        "{\"data\":{\"log_date\":\""
                                + hari
                                + "\",\"is_recorded\":true,"
                                + "\"is_locked\":false,\"method\":\"per_item\",\"note\":null,"
                                + "\"total_value_rupiah\":527000,\"total_weight_gram\":3100,"
                                + "\"total_items\":20,\"change_vs_last_week_percent\":12,"
                                + "\"products\":["
                                + produkSisa(7, "Croissant mentega", 28000, 6, 1100, "discarded")
                                + ","
                                + produkSisa(8, "Danish keju", 27000, 4, 600, "discarded")
                                + ","
                                + produkSisa(9, "Cinnamon roll", 30000, 2, 400, "discarded")
                                + ","
                                + produkSisa(10, "Roti gandum", 22000, 3, 500, "discarded")
                                + ","
                                + produkSisa(11, "Kopi susu botol", 25000, 5, 500, "discarded")
                                + "],\"other_items\":[]}}");
            }
            if (path.startsWith("/api/partner/stores/5/summary")) {
                // Contoh Figma M05: Senin sampai Minggu pekan ini, Sabtu tertinggi.
                java.time.LocalDate senin =
                        id.lifeoffoods.data.LaporanMingguan.senin(
                                java.time.LocalDate.now(java.time.ZoneOffset.ofHours(7)));
                long[] nilai = {60, 50, 74, 78, 101, 124, 79};
                StringBuilder harian = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    harian.append(i == 0 ? "" : ",")
                            .append("{\"date\":\"")
                            .append(senin.plusDays(i))
                            .append("\",\"wasted_value_rupiah\":")
                            .append(nilai[i] * 1000)
                            .append("}");
                }
                return json(
                        "{\"data\":{\"store\":{\"id\":5,\"name\":\"Kopi Kalyan SCBD\"},"
                                + "\"is_open\":true,\"closes_at\":\"21:00\","
                                + "\"minutes_until_close\":125,\"is_today_logged\":false,"
                                + "\"rescued_value_this_week_rupiah\":1240000,"
                                + "\"items_sold_this_week\":62,\"avg_daily_waste_gram\":4200,"
                                + "\"unsold_bags_last_7_days\":8,\"pending_orders_today\":3,"
                                + "\"daily\":["
                                + harian
                                + "],\"peak_day\":\"Sabtu\"}}");
            }
            if (path.startsWith("/api/partner/stores/5/listings")) {
                return json(jualanMitra(path.contains("type=menu_item")));
            }
            if (path.startsWith("/api/partner/stores/5/templates")) {
                return json(
                        "{\"data\":[{\"id\":2,\"name\":\"Tas Pastry Sore\","
                                + "\"content_hint\":\"empat sampai enam potong\","
                                + "\"price_rupiah\":18000,\"original_value_rupiah\":55000,"
                                + "\"default_qty\":4,\"pickup_start_time\":\"20:30:00\","
                                + "\"pickup_end_time\":\"21:00:00\",\"halal_label\":\"self_claim\"},"
                                + "{\"id\":3,\"name\":\"Tas Minuman Dingin\","
                                + "\"content_hint\":\"tiga botol\",\"price_rupiah\":15000,"
                                + "\"original_value_rupiah\":45000,\"default_qty\":3,"
                                + "\"pickup_start_time\":\"20:00:00\","
                                + "\"pickup_end_time\":\"21:00:00\",\"halal_label\":\"not_stated\"}]}");
            }
            if (path.startsWith("/api/partner/stores/5/products")) {
                return json(
                        "{\"data\":[{\"id\":7,\"name\":\"Croissant mentega\",\"unit\":\"pcs\","
                                + "\"price_rupiah\":28000,\"ingredients_text\":\"Tepung terigu,"
                                + " mentega, telur, susu\"},{\"id\":8,\"name\":\"Danish keju\","
                                + "\"unit\":\"pcs\",\"price_rupiah\":27000,\"ingredients_text\":"
                                + "\"Tepung terigu, keju, telur\"},{\"id\":9,\"name\":\"Cinnamon"
                                + " roll\",\"unit\":\"pcs\",\"price_rupiah\":30000,"
                                + "\"ingredients_text\":\"Tepung terigu, kayu manis, gula\"}]}");
            }
            if (path.equals("/api/partner/stores/5")) {
                return json(tokoDetail());
            }
            if (path.startsWith("/api/partner/stores/5/members")) {
                return json(
                        "{\"data\":[{\"id\":null,\"user_id\":3,\"name\":\"Kalyan Pratama\","
                                + "\"phone\":\"6281299887766\",\"role\":\"owner\","
                                + "\"is_store_owner\":true},{\"id\":7,\"user_id\":9,"
                                + "\"name\":\"Rina\",\"phone\":\"6281322445566\","
                                + "\"role\":\"cashier\",\"is_store_owner\":false}]}");
            }
            if (path.startsWith("/api/partner/stores?") || path.equals("/api/partner/stores")) {
                return json(
                        "{\"data\":[{\"id\":5,\"name\":\"Kopi Kalyan SCBD\",\"category\":\"cafe\","
                                + "\"address\":\"Jl. Jend. Sudirman\",\"photo_path\":null,"
                                + "\"is_temporarily_closed\":false,\"my_role\":\"owner\"}]}");
            }
            if (path.startsWith("/api/partner/stores/5/orders") && path.contains("days=")) {
                return json(riwayatM21());
            }
            if (path.startsWith("/api/partner/stores/5/suggestions")) {
                return json(saranProduksi());
            }
            if (path.startsWith("/api/partner/stores/5/balance/transactions")) {
                return json(transaksiSaldo());
            }
            if (path.startsWith("/api/partner/stores/5/balance")) {
                return json(
                        "{\"data\":{\"available_rupiah\":1186000,\"pending_rupiah\":0,"
                                + "\"lifetime_rupiah\":2086000,\"items_sold_this_week\":62,"
                                + "\"withdrawal\":{\"enabled\":false,\"reason\":\"Pencairan"
                                + " tersedia setelah masa uji coba.\"}}}");
            }
            if (path.startsWith("/api/partner/stores/5/orders")) {
                return json(pesananMitra(path.contains("status=history")));
            }
            if (path.startsWith("/api/pickup-codes/redeem")) {
                String body = r.getBody().readUtf8();
                if (body.contains("\"LF7Q2K\"")) {
                    return json(
                            "{\"data\":"
                                    + barisMitra(
                                            88,
                                            "completed",
                                            "Dara",
                                            "Tas Pastry Sore",
                                            1,
                                            18000,
                                            "cash",
                                            "Alergi kacang, tolong dipisah ya",
                                            "[{\"code\":\"kacang_tanah\",\"name\":\"Kacang tanah\",\"severity\":\"severe\"}]",
                                            "20:30",
                                            "21:00",
                                            "20:41")
                                    + "}");
                }
                return new MockResponse()
                        .setResponseCode(409)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"message\":\"Kode ini sudah dipakai pada 19.12.\"}");
            }
            if (path.startsWith("/api/orders?")) {
                return json(daftarPesanan(path.contains("status=history")));
            }
            if (path.startsWith("/api/orders/88")) {
                return json(
                        "{\"data\":{\"id\":88,\"code\":\"LOF-7Q2K9A\",\"status\":\"pending_pickup\","
                                + "\"pickup_code\":\"LF7Q2K\",\"pickup_code_status\":\"active\","
                                + "\"pickup_start\":\"2026-09-18T20:30:00+07:00\","
                                + "\"pickup_end\":\"2026-09-18T21:00:00+07:00\","
                                + "\"store\":{\"id\":5,\"name\":\"Kopi Kalyan\","
                                + "\"address\":\"Jl. Jend. Sudirman Kav 52, Lobi Utama, Jakarta"
                                + " Selatan\",\"latitude\":-6.2263,\"longitude\":106.8120},"
                                + "\"items\":[{\"listing_id\":41,\"title\":\"Croissant mentega\","
                                + "\"unit_price_rupiah\":9000,\"qty\":1,\"line_total_rupiah\":9000},"
                                + "{\"listing_id\":42,\"title\":\"Danish keju\","
                                + "\"unit_price_rupiah\":9000,\"qty\":1,\"line_total_rupiah\":9000}],"
                                + "\"item_count\":2,\"subtotal_rupiah\":18000,\"service_fee_rupiah\":0,"
                                + "\"discount_rupiah\":0,\"total_rupiah\":18000,"
                                + "\"payment_method\":\"cash\",\"payment_status\":\"unpaid\","
                                + "\"note\":null,\"placed_at\":\"2026-09-18T19:55:00+07:00\","
                                + "\"completed_at\":null,\"cancelled_at\":null}}");
            }
            return new MockResponse().setResponseCode(404).setBody("{\"message\":\"x\"}");
        }

        /** Profil toko contoh Figma M14: Senin sampai Jumat 07-21, Sabtu dan Minggu 08-22. */
        private static String tokoDetail() {
            StringBuilder jam = new StringBuilder();
            for (int hari = 0; hari <= 6; hari++) {
                boolean akhirPekan = hari == 0 || hari == 6;
                if (hari > 0) {
                    jam.append(',');
                }
                jam.append("{\"day_of_week\":").append(hari);
                jam.append(",\"is_closed\":false,\"open_time\":\"");
                jam.append(akhirPekan ? "08:00" : "07:00").append("\",\"close_time\":\"");
                jam.append(akhirPekan ? "22:00" : "21:00").append("\"}");
            }
            return "{\"data\":{\"id\":5,\"name\":\"Kopi Kalyan SCBD\",\"category\":\"cafe\","
                    + "\"address\":\"Jl. Jend. Sudirman Kav 52\",\"photo_url\":null,"
                    + "\"halal_label\":\"certified\",\"default_ingredients_text\":"
                    + "\"Susu, telur, gluten. Dapur juga mengolah kacang.\","
                    + "\"is_temporarily_closed\":false,\"hours\":["
                    + jam
                    + "],\"my_role\":\"owner\",\"available_balance_rupiah\":1186000,"
                    + "\"rating_average\":4.8,\"rating_count\":180}}";
        }

        /**
         * Laporan mingguan. Minggu ini mengikuti contoh Figma M07 (tujuh hari tercatat); minggu
         * lalu empat hari tercatat dan tiga celah.
         */
        private static String laporan(java.time.LocalDate senin, boolean contohFigma) {
            long[] ribu = {310, 280, 390, 420, 520, 610, 330};
            boolean[] celah = {false, true, false, true, false, false, true};
            StringBuilder harian = new StringBuilder();
            for (int i = 0; i < 7; i++) {
                boolean kosong = !contohFigma && celah[i];
                harian.append(i == 0 ? "" : ",")
                        .append("{\"date\":\"")
                        .append(senin.plusDays(i))
                        .append("\",\"wasted_value_rupiah\":")
                        .append(kosong ? "null" : String.valueOf(ribu[i] * 1000))
                        .append('}');
            }
            String teratas =
                    contohFigma
                            ? teratas(7, "Croissant mentega", 34, 952000)
                                    + ","
                                    + teratas(11, "Kopi susu botol", 21, 525000)
                                    + ","
                                    + teratas(8, "Danish keju", 18, 486000)
                                    + ","
                                    + teratas(10, "Roti gandum", 12, 264000)
                            : teratas(7, "Croissant mentega", 20, 560000)
                                    + ","
                                    + teratas(9, "Cinnamon roll", 9, 270000);
            return "{\"week_start\":\""
                    + senin
                    + "\",\"week_end\":\""
                    + senin.plusDays(6)
                    + "\","
                    + (contohFigma
                            ? "\"wasted_value_rupiah\":1620000,\"wasted_weight_gram\":11000,"
                                    + "\"logged_days\":7,\"rescued_value_rupiah\":1240000,"
                                    + "\"orders_count\":58,\"items_sold\":62,"
                                    + "\"unsold_value_rupiah\":2860000,\"wasted_change_percent\":-18,"
                            : "\"wasted_value_rupiah\":1830000,\"wasted_weight_gram\":0,"
                                    + "\"logged_days\":4,\"rescued_value_rupiah\":980000,"
                                    + "\"orders_count\":41,\"items_sold\":44,"
                                    + "\"unsold_value_rupiah\":2810000,\"wasted_change_percent\":null,")
                    + "\"top_wasted_products\":["
                    + teratas
                    + "],\"daily\":["
                    + harian
                    + "]}";
        }

        private static String teratas(long id, String nama, int qty, long nilai) {
            return "{\"product_id\":"
                    + id
                    + ",\"label\":\""
                    + nama
                    + "\",\"qty\":"
                    + qty
                    + ",\"weight_gram\":null,\"value_rupiah\":"
                    + nilai
                    + "}";
        }

        private static String produkSisa(
                long id, String nama, long harga, int qty, int gram, String tujuan) {
            return "{\"product_id\":"
                    + id
                    + ",\"name\":\""
                    + nama
                    + "\",\"unit\":\"pcs\","
                    + "\"price_rupiah\":"
                    + harga
                    + ",\"unit_value_rupiah\":"
                    + harga
                    + ",\"qty\":"
                    + qty
                    + ",\"weight_gram\":"
                    + gram
                    + ",\"disposition\":\""
                    + tujuan
                    + "\"}";
        }

        /** Contoh Figma M10 (tas kejutan) dan M17 (menu satuan). */
        private static String jualanMitra(boolean menu) {
            if (!menu) {
                return "{\"data\":["
                        + barisJualan(
                                31,
                                "surprise_bag",
                                "active",
                                "Tas Pastry Sore",
                                18000,
                                6,
                                2,
                                2,
                                "20:30",
                                "21:00",
                                "[]")
                        + ","
                        + barisJualan(
                                32,
                                "surprise_bag",
                                "sold_out",
                                "Tas Minuman Dingin",
                                15000,
                                4,
                                1,
                                3,
                                "20:00",
                                "21:00",
                                "[]")
                        + ","
                        + barisJualan(
                                33,
                                "surprise_bag",
                                "paused",
                                "Tas campur",
                                22000,
                                3,
                                0,
                                0,
                                "21:00",
                                "21:30",
                                "[]")
                        + "]}";
            }
            return "{\"data\":["
                    + barisJualan(
                            41,
                            "menu_item",
                            "active",
                            "Croissant mentega",
                            9000,
                            4,
                            0,
                            2,
                            "20:30",
                            "21:00",
                            "[{\"code\":\"susu\",\"presence\":\"contains\"},"
                                    + "{\"code\":\"gluten\",\"presence\":\"contains\"}]")
                    + ","
                    + barisJualan(
                            43,
                            "menu_item",
                            "active",
                            "Cinnamon roll",
                            11000,
                            2,
                            0,
                            0,
                            "20:30",
                            "21:00",
                            "[{\"code\":\"kacang_tanah\",\"presence\":\"may_contain\"}]")
                    + ","
                    + barisJualan(
                            44,
                            "menu_item",
                            "sold_out",
                            "Kopi susu botol",
                            12000,
                            5,
                            0,
                            5,
                            "20:00",
                            "21:00",
                            "[{\"code\":\"susu\",\"presence\":\"contains\"}]")
                    + "]}";
        }

        private static String barisJualan(
                long id,
                String tipe,
                String status,
                String judul,
                long harga,
                int total,
                int dipesan,
                int terjual,
                String mulai,
                String akhir,
                String alergen) {
            String hari = java.time.LocalDate.now(java.time.ZoneOffset.ofHours(7)).toString();
            return "{\"id\":"
                    + id
                    + ",\"type\":\""
                    + tipe
                    + "\",\"status\":\""
                    + status
                    + "\",\"title\":\""
                    + judul
                    + "\",\"product_id\":null,\"template_id\":null,"
                    + "\"price_rupiah\":"
                    + harga
                    + ",\"original_value_rupiah\":null,"
                    + "\"qty_total\":"
                    + total
                    + ",\"qty_reserved\":"
                    + dipesan
                    + ",\"qty_sold\":"
                    + terjual
                    + ",\"qty_remaining\":"
                    + Math.max(0, total - dipesan - terjual)
                    + ",\"potential_income_rupiah\":"
                    + harga * total
                    + ",\"pickup_start\":\""
                    + hari
                    + "T"
                    + mulai
                    + ":00+07:00\""
                    + ",\"pickup_end\":\""
                    + hari
                    + "T"
                    + akhir
                    + ":00+07:00\""
                    + ",\"ingredients_text\":\"Tepung terigu\",\"halal_label\":\"self_claim\","
                    + "\"published_at\":null,\"allergens\":"
                    + alergen
                    + "}";
        }

        /** Contoh Figma K17: tiga hari ini, satu kemarin (jenis yang memang dikirim server). */
        private static String notifikasiK17() {
            java.time.OffsetDateTime kini =
                    java.time.OffsetDateTime.now(java.time.ZoneOffset.ofHours(7));
            return "{\"data\":["
                    + notif(
                            4,
                            "pengingat_ambil",
                            "Pesananmu siap diambil",
                            "Tas Pastry Sore di Kopi Kalyan. Tunjukkan kode LF7Q2K ke kasir.",
                            kini.minusMinutes(5),
                            88)
                    + ","
                    + notif(
                            3,
                            "mitra_favorit_memasang",
                            "Mitra favoritmu memasang jualan",
                            "Bakerman Blok M baru saja membuka menu satuan roti sore.",
                            kini.minusMinutes(32),
                            0)
                    + ","
                    + notif(
                            2,
                            "porsi_terselamatkan",
                            "1 porsi terselamatkan",
                            "Pesanan Tas Minuman Dingin selesai. Totalmu sekarang 16 porsi.",
                            kini.minusDays(1).withHour(20).withMinute(12),
                            0)
                    + "],\"unread_count\":2,\"current_page\":1,\"last_page\":1}";
        }

        private static String notif(
                long id,
                String jenis,
                String judul,
                String isi,
                java.time.OffsetDateTime waktu,
                long pesanan) {
            return "{\"id\":"
                    + id
                    + ",\"type\":\""
                    + jenis
                    + "\",\"title\":\""
                    + judul
                    + "\",\"body\":\""
                    + isi
                    + "\",\"data\":"
                    + (pesanan > 0 ? "{\"screen\":\"K14\",\"order_id\":" + pesanan + "}" : "null")
                    + ",\"is_read\":false,\"created_at\":\""
                    + waktu.withSecond(0).withNano(0)
                    + "\"}";
        }

        /** Contoh Figma M21: kemarin dua pesanan, dua hari lalu dua pesanan. */
        private static String riwayatM21() {
            String[] baris = {
                riwayat(
                        1,
                        80,
                        "completed",
                        "Nadia",
                        "Tas Minuman Dingin",
                        1,
                        15000,
                        "20:12",
                        "Q7ZR2A"),
                riwayat(1, 77, "no_show", "Rafi", "Tas Pastry Sore", 1, 18000, null, "H2W9LC"),
                riwayat(
                        2,
                        71,
                        "completed",
                        "Sinta",
                        "Croissant mentega",
                        2,
                        18000,
                        "19:44",
                        "T5NB1X"),
                riwayat(2, 70, "completed", "Andi", "Tas campur", 1, 22000, "20:51", "V8QM3R")
            };
            return "{\"data\":["
                    + String.join(",", baris)
                    + "],\"current_page\":1,\"last_page\":2,"
                    + "\"summary\":{\"completed\":62,\"no_show\":3,\"cancelled\":1}}";
        }

        private static String riwayat(
                int hariLalu,
                long id,
                String status,
                String pembeli,
                String judul,
                int qty,
                long total,
                String selesai,
                String kode) {
            java.time.LocalDate ini = java.time.LocalDate.now(java.time.ZoneOffset.ofHours(7));
            String isi =
                    barisMitra(
                            id, status, pembeli, judul, qty, total, "cash", null, "[]", "20:00",
                            "21:00", selesai);
            return isi.replace(ini.toString(), ini.minusDays(hariLalu).toString())
                    .replaceFirst("\\}$", ",\"pickup_code\":\"" + kode + "\"}");
        }

        /** Contoh Figma M08: dua saran sudah dipakai, satu belum. */
        private static String saranProduksi() {
            return "{\"data\":{\"suggested_for_date\":\"2026-09-19\",\"weekday\":\"Sabtu\","
                    + "\"sample_days\":4,\"has_enough_data\":true,"
                    + "\"products_missing_production_qty\":0,"
                    + "\"total_saving_per_week_rupiah\":266000,\"items\":["
                    + saran(1, "Croissant mentega", 24, 20, 6, 112000, "accepted")
                    + ","
                    + saran(2, "Kopi susu botol", 30, 26, 5, 100000, "accepted")
                    + ","
                    + saran(3, "Danish keju", 18, 16, 4, 54000, "new")
                    + "]}}";
        }

        private static String saran(
                long id, String nama, int biasa, int saran, int sisa, long hemat, String status) {
            return "{\"id\":"
                    + id
                    + ",\"product_id\":"
                    + id
                    + ",\"name\":\""
                    + nama
                    + "\",\"unit\":\"potong\",\"current_production\":"
                    + biasa
                    + ",\"suggested_production\":"
                    + saran
                    + ",\"reduce_by\":"
                    + (biasa - saran)
                    + ",\"avg_waste_qty\":"
                    + sisa
                    + ",\"estimated_saving_per_week_rupiah\":"
                    + hemat
                    + ",\"status\":\""
                    + status
                    + "\"}";
        }

        /** Contoh Figma M13. */
        private static String transaksiSaldo() {
            java.time.LocalDate ini = java.time.LocalDate.now(java.time.ZoneOffset.ofHours(7));
            String kemarin = ini.minusDays(1).toString();
            return "{\"data\":["
                    + transaksi(4, "sale", 18000, "Tas Pastry Sore", "LF7Q2K", ini + "T20:34")
                    + ","
                    + transaksi(3, "sale", 15000, "Tas Minuman Dingin", "Q7ZR2A", ini + "T20:12")
                    + ","
                    + transaksi(2, "sale", 29000, "Menu satuan", "M3K8PD", kemarin + "T20:45")
                    + ","
                    + transaksi(1, "adjustment", 0, null, null, kemarin + "T09:00")
                    + "],\"current_page\":1,\"last_page\":1}";
        }

        private static String transaksi(
                long id, String tipe, long nominal, String judul, String kode, String waktu) {
            return "{\"id\":"
                    + id
                    + ",\"type\":\""
                    + tipe
                    + "\",\"amount_rupiah\":"
                    + nominal
                    + ",\"balance_after_rupiah\":0,\"title\":"
                    + (judul == null ? "null" : "\"" + judul + "\"")
                    + ",\"pickup_code\":"
                    + (kode == null ? "null" : "\"" + kode + "\"")
                    + ",\"description\":"
                    + (judul == null ? "\"Saldo awal pilot\"" : "null")
                    + ",\"created_at\":\""
                    + waktu
                    + ":00+07:00\"}";
        }

        /** Contoh Figma M11: tiga menunggu, satu diambil, satu tidak diambil. */
        private static String pesananMitra(boolean riwayat) {
            if (!riwayat) {
                return "{\"data\":["
                        + barisMitra(
                                88,
                                "pending_pickup",
                                "Dara",
                                "Tas Pastry Sore",
                                1,
                                18000,
                                "cash",
                                "Alergi kacang, tolong dipisah ya",
                                "[{\"code\":\"kacang_tanah\",\"name\":\"Kacang tanah\","
                                        + "\"severity\":\"severe\"}]",
                                "20:30",
                                "21:00",
                                null)
                        + ","
                        + barisMitra(
                                89,
                                "pending_pickup",
                                "Bima",
                                "Croissant mentega",
                                2,
                                18000,
                                "qris_static",
                                null,
                                "[{\"code\":\"susu\",\"name\":\"Susu\",\"severity\":\"avoid\"}]",
                                "20:30",
                                "21:00",
                                null)
                        + ","
                        + barisMitra(
                                90,
                                "pending_pickup",
                                "Sari",
                                "Tas Roti Malam",
                                1,
                                15000,
                                "cash",
                                null,
                                "[]",
                                "21:00",
                                "21:30",
                                null)
                        + "],\"current_page\":1,\"last_page\":1}";
            }
            return "{\"data\":["
                    + barisMitra(
                            80,
                            "completed",
                            "Nadia",
                            "Tas Minuman Dingin",
                            1,
                            12000,
                            "cash",
                            null,
                            "[]",
                            "20:00",
                            "21:00",
                            "20:12")
                    + ","
                    + barisMitra(
                            77,
                            "no_show",
                            "Rafi",
                            "Tas Pastry Sore",
                            1,
                            18000,
                            "cash",
                            null,
                            "[]",
                            "19:30",
                            "20:30",
                            null)
                    + "],\"current_page\":1,\"last_page\":1}";
        }

        private static String barisMitra(
                long id,
                String status,
                String pembeli,
                String judul,
                int qty,
                long total,
                String bayar,
                String catatan,
                String alergi,
                String mulai,
                String akhir,
                String selesai) {
            String hari = java.time.LocalDate.now(java.time.ZoneOffset.ofHours(7)).toString();
            return "{\"id\":"
                    + id
                    + ",\"code\":\"LOF-"
                    + id
                    + "\",\"status\":\""
                    + status
                    + "\",\"buyer_name\":\""
                    + pembeli
                    + "\",\"items\":[{\"title\":\""
                    + judul
                    + "\",\"qty\":"
                    + qty
                    + ",\"line_total_rupiah\":"
                    + total
                    + "}],"
                    + "\"item_count\":"
                    + qty
                    + ",\"total_rupiah\":"
                    + total
                    + ",\"payment_method\":\""
                    + bayar
                    + "\",\"payment_status\":\"unpaid\","
                    + "\"note\":"
                    + (catatan == null ? "null" : "\"" + catatan + "\"")
                    + ",\"allergen_snapshot\":"
                    + alergi
                    + ",\"pickup_start\":\""
                    + hari
                    + "T"
                    + mulai
                    + ":00+07:00\""
                    + ",\"pickup_end\":\""
                    + hari
                    + "T"
                    + akhir
                    + ":00+07:00\""
                    + ",\"placed_at\":\""
                    + hari
                    + "T18:00:00+07:00\",\"completed_at\":"
                    + (selesai == null ? "null" : "\"" + hari + "T" + selesai + ":00+07:00\"")
                    + "}";
        }

        /**
         * Contoh Figma K15. Tanggal dihitung dari hari ini (WIB) supaya label "Hari ini" dan
         * "Kemarin" tampil seperti di Figma, kapan pun tes dijalankan.
         */
        private static String daftarPesanan(boolean riwayat) {
            java.time.LocalDate hari = java.time.LocalDate.now(java.time.ZoneOffset.ofHours(7));
            if (!riwayat) {
                return "{\"data\":["
                        + barisPesanan(88, "pending_pickup", "Kopi Kalyan", 2, 18000, hari, "20:30")
                        + ","
                        + barisPesanan(
                                89, "pending_pickup", "Bakerman Blok M", 1, 25000, hari, "19:00")
                        + "],\"current_page\":1,\"last_page\":1}";
            }
            return "{\"data\":["
                    + barisPesanan(
                            80,
                            "completed",
                            "Toko Kopi Ashta",
                            1,
                            15000,
                            hari.minusDays(1),
                            "20:10")
                    + ","
                    + barisPesanan(
                            81, "completed", "Kopi Kalyan", 2, 18000, hari.minusDays(2), "20:30")
                    + ","
                    + barisPesanan(
                            77, "no_show", "Dapur Senopati", 1, 20000, hari.minusDays(3), "21:05")
                    + ","
                    + barisPesanan(
                            70, "cancelled", "Roti Kita", 2, 22000, hari.minusDays(9), "19:30")
                    + "],\"current_page\":1,\"last_page\":1}";
        }

        private static String barisPesanan(
                long id,
                String status,
                String toko,
                int jumlah,
                long total,
                java.time.LocalDate hari,
                String jam) {
            String mulai = hari + "T" + jam + ":00+07:00";
            return "{\"id\":"
                    + id
                    + ",\"code\":\"LOF-"
                    + id
                    + "Q2K9A\",\"status\":\""
                    + status
                    + "\",\"store_name\":\""
                    + toko
                    + "\",\"item_count\":"
                    + jumlah
                    + ",\"total_rupiah\":"
                    + total
                    + ",\"pickup_start\":\""
                    + mulai
                    + "\",\"pickup_end\":\""
                    + mulai
                    + "\",\"placed_at\":\""
                    + mulai
                    + "\",\"review_rating\":"
                    + (id == 81 ? "5" : "null")
                    + ",\"can_review\":"
                    + ("completed".equals(status))
                    + "}";
        }

        /** Contoh Figma K12 (tas 31) dan K13 (menu 41 + 42), dipilih dari body permintaan. */
        private static String pratinjau(String body) {
            boolean menu = body.contains("\"listing_id\":41");
            String items =
                    menu
                            ? "{\"listing_id\":41,\"title\":\"Croissant mentega\",\"qty\":1,"
                                    + "\"unit_price_rupiah\":9000,\"line_total_rupiah\":9000},"
                                    + "{\"listing_id\":42,\"title\":\"Danish keju\",\"qty\":1,"
                                    + "\"unit_price_rupiah\":9000,\"line_total_rupiah\":9000}"
                            : "{\"listing_id\":31,\"title\":\"Tas Pastry Sore\",\"qty\":1,"
                                    + "\"unit_price_rupiah\":18000,\"line_total_rupiah\":18000}";
            long total = 18000;
            return "{\"store\":{\"id\":5,\"name\":\"Kopi Kalyan\","
                    + "\"address\":\"Jl. Jend. Sudirman Kav 52, Lobi Utama, Jakarta Selatan\"},"
                    + "\"items\":["
                    + items
                    + "],\"pickup_start\":\"2026-09-18T20:30:00+07:00\","
                    + "\"pickup_end\":\"2026-09-18T21:00:00+07:00\","
                    + "\"subtotal_rupiah\":"
                    + total
                    + ",\"service_fee_rupiah\":0,\"discount_rupiah\":0,\"total_rupiah\":"
                    + total
                    + ",\"allergen_warnings\":"
                    + (menu
                            ? "[]"
                            : "[{\"listing_id\":31,\"code\":\"kacang_tanah\","
                                    + "\"name\":\"Kacang tanah\",\"presence\":\"may_contain\","
                                    + "\"severity\":\"avoid\"}]")
                    + "}";
        }

        /** Contoh Figma K16: empat mitra dengan empat jenis chip status. */
        private static String favoritK16() {
            return "{\"data\":["
                    + favorit(5, "Kopi Kalyan", "cafe", 0.38, "21:00", 2, false, null)
                    + ","
                    + favorit(6, "Bakerman Blok M", "bakery", 1.1, "20:00", 0, true, null)
                    + ","
                    + favorit(7, "Dapur Senopati", "catering", 0.65, "21:30", 0, false, null)
                    + ","
                    + favorit(8, "Toko Kopi Ashta", "cafe", 0.9, "21:00", 0, false, "19:00")
                    + "]}";
        }

        private static String favorit(
                long id,
                String nama,
                String kategori,
                double km,
                String tutup,
                int tas,
                boolean menu,
                String biasanya) {
            return "{\"id\":"
                    + id
                    + ",\"name\":\""
                    + nama
                    + "\",\"category\":\""
                    + kategori
                    + "\",\"photo_url\":null,\"distance_km\":"
                    + km
                    + ",\"closes_at\":\""
                    + tutup
                    + "\",\"is_temporarily_closed\":false,\"available_bags\":"
                    + tas
                    + ",\"has_menu_available\":"
                    + menu
                    + ",\"usual_publish_time\":"
                    + (biasanya == null ? "null" : "\"" + biasanya + "\"")
                    + "}";
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
                    + ",\"is_closed\":0},\"rating_average\":4.8,\"rating_count\":180}}";
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
                    + "\",\"category\":\"cafe\""
                    // Sebar toko di sekitar SCBD supaya penanda K08 tidak bertumpuk.
                    + ",\"latitude\":"
                    + (-6.2250 + ((id % 3) - 1) * 0.0035)
                    + ",\"longitude\":"
                    + (106.8090 + ((id % 4) - 1.5) * 0.0030)
                    + "}}";
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
