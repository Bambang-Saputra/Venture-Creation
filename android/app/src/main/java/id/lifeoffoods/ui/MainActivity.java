package id.lifeoffoods.ui;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NavigationRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.data.api.model.AuthResponse;
import id.lifeoffoods.databinding.ActivityMainBinding;

/**
 * Satu-satunya activity. Graf navigasi dipilih sesuai peran (peta layar, catatan K01): nav_awal
 * untuk yang belum masuk, nav_konsumen atau nav_mitra sesudahnya. Tujuan awal graf peran ikut
 * ditentukan di sini: layar masuk kalau belum ada sesi, beranda kalau sudah.
 */
public class MainActivity extends AppCompatActivity {

    private static final String KUNCI_GRAF = "graf_aktif";
    private static final String KUNCI_AWAL = "awal_aktif";

    private NavController nav;
    private SesiPengguna sesi;
    @NavigationRes private int grafAktif;
    @IdRes private int awalAktif;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-edge di semua versi, sama dengan perilaku wajib Android 15+ (targetSdk 36).
        // Jarak dari status bar, navbar, dan keyboard diatur tiap layar lewat SisiAman.
        EdgeToEdge.enable(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment host =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        nav = host.getNavController();
        sesi = ((LofApp) getApplication()).sesi();

        // Graf dipasang lewat kode, jadi NavHostFragment tidak memulihkannya sendiri setelah
        // rotasi atau proses dipulihkan. Pasang ulang graf yang sama; NavController lalu
        // memulihkan tumpukan layarnya dari state yang tersimpan.
        if (savedInstanceState != null && savedInstanceState.getInt(KUNCI_GRAF) != 0) {
            pasangGraf(
                    savedInstanceState.getInt(KUNCI_GRAF), savedInstanceState.getInt(KUNCI_AWAL));
        } else if (sesi.sudahMasuk()) {
            String peran = sesi.peran();
            pasangGraf(
                    grafUntuk(peran),
                    sesi.perluProfil() ? R.id.k04_lengkapi_profil : berandaUntuk(peran));
        } else {
            pasangGraf(R.navigation.nav_awal, R.id.k01_pilih_peran);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KUNCI_GRAF, grafAktif);
        outState.putInt(KUNCI_AWAL, awalAktif);
    }

    /** Dari K01: buka layar masuk konsumen (K02) atau mitra (M01). */
    public void bukaAlur(String peran) {
        pasangGraf(
                grafUntuk(peran),
                SesiPengguna.PERAN_MITRA.equals(peran) ? R.id.m01_masuk : R.id.k02_masuk);
    }

    /**
     * Dari K03, M02, atau tombol Google: simpan sesi lalu buka layar pertama sesudah masuk. Graf
     * dipasang ulang supaya tombol kembali tidak membawa pengguna ke layar masuk lagi.
     */
    public void selesaiMasuk(AuthResponse hasil) {
        String peran = hasil.user.role;
        boolean perluProfil =
                SesiPengguna.PERAN_KONSUMEN.equals(peran)
                        && (hasil.isNewUser || hasil.user.name == null);
        sesi.simpan(hasil.token, peran, perluProfil);
        pasangGraf(grafUntuk(peran), perluProfil ? R.id.k04_lengkapi_profil : berandaUntuk(peran));
    }

    /**
     * Dari K05 (Simpan atau Lewati): onboarding selesai, buka K06 lalu beranda tanpa riwayat
     * K04/K05.
     */
    public void selesaiOnboarding() {
        pasangGraf(R.navigation.nav_konsumen, R.id.k06_onboarding);
    }

    /** Token ditolak server (401): hapus sesi lokal lalu kembali ke K01. */
    public void sesiBerakhir() {
        sesi.hapus();
        kembaliKeAwal();
    }

    /** Kembali ke K01, misalnya setelah logout atau token ditolak (401). */
    public void kembaliKeAwal() {
        pasangGraf(R.navigation.nav_awal, R.id.k01_pilih_peran);
    }

    private void pasangGraf(@NavigationRes int grafId, @IdRes int tujuanAwal) {
        NavGraph graf = nav.getNavInflater().inflate(grafId);
        graf.setStartDestination(tujuanAwal);
        nav.setGraph(graf, null);
        grafAktif = grafId;
        awalAktif = tujuanAwal;
    }

    @IdRes
    private static int berandaUntuk(@Nullable String peran) {
        return SesiPengguna.PERAN_MITRA.equals(peran) ? R.id.m05_dashboard : R.id.k07_beranda;
    }

    @NavigationRes
    private static int grafUntuk(@Nullable String peran) {
        return SesiPengguna.PERAN_MITRA.equals(peran)
                ? R.navigation.nav_mitra
                : R.navigation.nav_konsumen;
    }
}
