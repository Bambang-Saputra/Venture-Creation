package id.lifeoffoods.ui.pesanan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.DaftarPesanan;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.PesananRingkasDto;
import id.lifeoffoods.databinding.FragmentPesananSayaBinding;
import id.lifeoffoods.databinding.ItemKartuPesananBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * K15 Pesanan saya (PRD-08). Tab Aktif (pending_pickup, jam ambil terdekat di atas) dan Riwayat.
 * Ketuk kartu mana pun membuka K14: pesanan aktif menampilkan kode, yang sudah lewat menampilkan
 * keadaan akhirnya tanpa kode.
 */
public class PesananSayaFragment extends Fragment {

    /** Mulai memuat halaman berikutnya saat tersisa sekian kartu di bawah layar. */
    private static final int SISA_SEBELUM_MUAT = 4;

    private static final DateTimeFormatter TANGGAL =
            DateTimeFormatter.ofPattern("d MMM", new Locale("id", "ID"));
    private static final DateTimeFormatter TANGGAL_LENGKAP =
            DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("id", "ID"));

    private FragmentPesananSayaBinding binding;
    private PesananSayaViewModel vm;
    private BaseListAdapter<PesananRingkasDto, ItemKartuPesananBinding> adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPesananSayaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PesananSayaViewModel.class);

        SisiAman.atas(binding.header);
        SisiAman.bawah(binding.nav.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);

        adapter =
                new BaseListAdapter<>(ItemKartuPesananBinding::inflate, this::isiKartu, p -> p.id);
        binding.daftar.setAdapter(adapter);
        binding.daftar.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                        LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                        if (dy > 0
                                && lm != null
                                && lm.findLastVisibleItemPosition()
                                        >= adapter.getItemCount() - SISA_SEBELUM_MUAT) {
                            vm.muatBerikutnya();
                        }
                    }
                });

        binding.tabAktif.setOnClickListener(v -> vm.pilihTab(PesananSayaViewModel.Tab.AKTIF));
        binding.tabRiwayat.setOnClickListener(v -> vm.pilihTab(PesananSayaViewModel.Tab.RIWAYAT));
        binding.tombolBeranda.setOnClickListener(v -> keBeranda());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());

        binding.nav.tabPesanan.setSelected(true);
        binding.nav.tabBeranda.setOnClickListener(v -> keBeranda());
        binding.nav.tabFavorit.setOnClickListener(v -> buka(R.id.k16_favorit));
        binding.nav.tabProfil.setOnClickListener(v -> buka(R.id.k18_profil));

        vm.tab.observe(getViewLifecycleOwner(), this::tandaiTab);
        vm.daftar.observe(
                getViewLifecycleOwner(),
                d -> {
                    adapter.submitList(d);
                    tampilkanKeadaan();
                });
        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanKeadaan());
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.sesiBerakhir.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        ((MainActivity) requireActivity()).sesiBerakhir();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Status bisa berubah di K14 (dibatalkan, ditukar kasir) atau di toko selama layar
        // tertutup.
        vm.muatUlang();
    }

    private void tandaiTab(PesananSayaViewModel.Tab t) {
        boolean aktif = t == PesananSayaViewModel.Tab.AKTIF;
        gayaTab(binding.tabAktif, aktif);
        gayaTab(binding.tabRiwayat, !aktif);
    }

    private void gayaTab(TextView tv, boolean terpilih) {
        tv.setSelected(terpilih);
        tv.setBackgroundResource(terpilih ? R.drawable.bg_tab_aktif : 0);
        tv.setTextColor(
                ContextCompat.getColor(
                        requireContext(), terpilih ? R.color.teks_merek : R.color.teks_sekunder));
    }

    private void tampilkanKeadaan() {
        PesananSayaViewModel.Status s = vm.status.getValue();
        List<PesananRingkasDto> d = vm.daftar.getValue();
        boolean kosong = d == null || d.isEmpty();
        boolean siap = s == PesananSayaViewModel.Status.SIAP;

        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == PesananSayaViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == PesananSayaViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        binding.daftar.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);

        boolean tampilKosong = siap && kosong;
        binding.kosong.setVisibility(tampilKosong ? View.VISIBLE : View.GONE);
        if (tampilKosong) {
            boolean aktif = vm.tab.getValue() != PesananSayaViewModel.Tab.RIWAYAT;
            binding.kosongJudul.setText(
                    aktif ? R.string.k15_kosong_aktif_judul : R.string.k15_kosong_riwayat_judul);
            binding.kosongIsi.setText(
                    aktif ? R.string.k15_kosong_aktif_isi : R.string.k15_kosong_riwayat_isi);
            binding.tombolBeranda.setVisibility(aktif ? View.VISIBLE : View.GONE);
        }
    }

    private void isiKartu(ItemKartuPesananBinding b, PesananRingkasDto p) {
        b.judul.setText(p.storeName);
        String jumlah =
                getResources()
                        .getQuantityString(R.plurals.k15_jumlah_item, p.itemCount, p.itemCount);
        b.keterangan.setText(p.code == null ? jumlah : jumlah + " · " + p.code);
        b.total.setText(FormatTampilan.rupiah(p.totalRupiah));

        DaftarPesanan.Jenis jenis = DaftarPesanan.jenis(p.status);
        switch (jenis) {
            case SELESAI:
                pil(
                        b.status,
                        R.string.k15_status_selesai,
                        R.drawable.bg_pil_isian,
                        R.color.teks_kuat);
                break;
            case DIBATALKAN:
                pil(
                        b.status,
                        R.string.k15_status_dibatalkan,
                        R.drawable.bg_pil_isian,
                        R.color.teks_kuat);
                break;
            case TIDAK_DIAMBIL:
                pil(
                        b.status,
                        R.string.k15_status_tidak_diambil,
                        R.drawable.bg_pil_bahaya,
                        R.color.tanda_bahaya_teks);
                break;
            default:
                pil(
                        b.status,
                        R.string.k15_status_menunggu,
                        R.drawable.bg_pil_merek,
                        R.color.tanda_hemat_teks);
                break;
        }

        String hari = hari(p.pickupStart);
        String jam = FormatTampilan.jam(p.pickupStart);
        String waktuAmbil =
                jenis == DaftarPesanan.Jenis.MENUNGGU
                        ? getString(R.string.k15_waktu_ambil, hari, jam)
                        : getString(R.string.k15_waktu_riwayat, hari, jam);
        // Tanggal dan jam pesanan dibuat, supaya riwayat bisa ditelusuri per tanggal.
        String dipesan = tanggalPesan(p.placedAt);
        b.waktu.setText(
                dipesan == null
                        ? waktuAmbil
                        : waktuAmbil + "\n" + getString(R.string.k15_dipesan, dipesan));

        b.getRoot()
                .setContentDescription(
                        getString(R.string.k15_buka_pesanan, p.storeName, b.status.getText()));
        b.getRoot().setOnClickListener(v -> keKodePickup(p.id));
    }

    private void pil(TextView tv, @StringRes int teks, @DrawableRes int latar, int warna) {
        tv.setText(teks);
        tv.setBackgroundResource(latar);
        tv.setTextColor(ContextCompat.getColor(requireContext(), warna));
    }

    /** "Hari ini", "Kemarin", "Besok", "3 hari lalu", lalu tanggal pendek setelah seminggu. */
    private String hari(@Nullable String iso) {
        Long selisih = DaftarPesanan.selisihHari(iso, hariIniDiZonaServer(iso));
        if (selisih == null) {
            return "";
        }
        if (selisih == 0) {
            return getString(R.string.k15_hari_ini);
        }
        if (selisih == 1) {
            return getString(R.string.k15_kemarin);
        }
        if (selisih == -1) {
            return getString(R.string.k15_besok);
        }
        if (selisih > 1 && selisih < 7) {
            return getString(R.string.k15_hari_lalu, selisih.intValue());
        }
        try {
            return OffsetDateTime.parse(iso).format(TANGGAL);
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    /** "3 Okt 2026, 19.42" dari placed_at, di zona waktu server. */
    @Nullable
    private static String tanggalPesan(@Nullable String iso) {
        if (iso == null) {
            return null;
        }
        try {
            OffsetDateTime t = OffsetDateTime.parse(iso);
            return t.format(TANGGAL_LENGKAP) + ", " + FormatTampilan.jam(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** "Hari ini" dihitung di zona waktu server (WIB), sama seperti jam yang ditampilkan. */
    private static LocalDate hariIniDiZonaServer(@Nullable String iso) {
        try {
            return OffsetDateTime.now(OffsetDateTime.parse(iso).getOffset()).toLocalDate();
        } catch (DateTimeParseException | NullPointerException e) {
            return LocalDate.now();
        }
    }

    private void keKodePickup(long idPesanan) {
        Bundle a = new Bundle();
        a.putLong(KodePickupFragment.ARG_ID_PESANAN, idPesanan);
        NavHostFragment.findNavController(this).navigate(R.id.k14_kode_pickup, a);
    }

    private void keBeranda() {
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.k07_beranda, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.k07_beranda, null, opsi);
    }

    private void buka(int tujuan) {
        NavHostFragment.findNavController(this).navigate(tujuan);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.daftar.setAdapter(null);
        binding = null;
    }
}
