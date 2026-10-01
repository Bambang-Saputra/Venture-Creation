package id.lifeoffoods.ui.beranda;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.databinding.FragmentBerandaBinding;
import id.lifeoffoods.databinding.IncludeUbinKategoriBinding;
import id.lifeoffoods.databinding.ItemKartuFlashBinding;
import id.lifeoffoods.databinding.ItemKartuTasBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.filter.FilterBundle;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;

/**
 * K07 Beranda konsumen. Kolom cari membuka K09 Filter; hasilnya kembali lewat setFragmentResult.
 * Tujuan lain (K08 peta, K16-K18) masih penanda di nav_konsumen sampai layarnya dibuat.
 */
public class BerandaFragment extends Fragment {

    public static final String ARG_LISTING_ID = "listing_id";

    private FragmentBerandaBinding binding;
    private BerandaViewModel vm;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hasil K09. Didaftarkan di onCreate supaya tetap diterima walau view K07 sudah dibuang
        // selama K09 tampil; dikirim saat K07 kembali STARTED.
        getParentFragmentManager()
                .setFragmentResultListener(
                        FilterBundle.HASIL,
                        this,
                        (kunci, hasil) -> {
                            FilterJualan f = FilterBundle.dari(hasil);
                            if (f != null) {
                                new ViewModelProvider(this)
                                        .get(BerandaViewModel.class)
                                        .pakaiFilter(f);
                            }
                        });
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentBerandaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(BerandaViewModel.class);
        SisiAman.atas(binding.header);
        SisiAman.bawah(binding.nav.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);

        pasangKategori(
                binding.kategoriBakery, "bakery", R.drawable.ic_roti, R.string.kategori_bakery);
        pasangKategori(binding.kategoriKafe, "cafe", R.drawable.ic_kopi, R.string.kategori_kafe);
        pasangKategori(
                binding.kategoriRestoran,
                "resto",
                R.drawable.ic_restoran,
                R.string.kategori_restoran);
        pasangKategori(
                binding.kategoriKatering,
                "catering",
                R.drawable.ic_paket,
                R.string.kategori_katering);
        pasangKategori(
                binding.kategoriSwalayan,
                "grocery",
                R.drawable.ic_toko,
                R.string.kategori_swalayan);

        BaseListAdapter<ListingDto, ItemKartuFlashBinding> adapterSegera =
                new BaseListAdapter<>(
                        ItemKartuFlashBinding::inflate, this::isiKartuFlash, l -> l.id);
        binding.daftarSegera.setAdapter(adapterSegera);
        BaseListAdapter<ListingDto, ItemKartuTasBinding> adapterTerdekat =
                new BaseListAdapter<>(ItemKartuTasBinding::inflate, this::isiKartuTas, l -> l.id);
        binding.daftarTerdekat.setAdapter(adapterTerdekat);

        binding.kolomCari.setOnClickListener(v -> bukaFilter());
        binding.penandaFilter.setOnClickListener(v -> bukaFilter());
        binding.tombolUbahFilter.setOnClickListener(
                v -> {
                    FilterJualan f = vm.filter.getValue();
                    if (f != null && f.radiusKm != null) {
                        vm.perluasJarak();
                    } else {
                        bukaFilter();
                    }
                });
        binding.tombolPeta.setOnClickListener(v -> buka(R.id.k08_peta, null));
        binding.lihatSemua.setOnClickListener(v -> buka(R.id.k08_peta, null));
        binding.tombolNotifikasi.setOnClickListener(v -> buka(R.id.k17_notifikasi, null));
        binding.avatar.setOnClickListener(v -> buka(R.id.k18_profil, null));
        // Mengubah area dilakukan di K19 Edit profil.
        binding.lokasi.setOnClickListener(v -> buka(R.id.k18_profil, null));
        binding.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());

        binding.nav.tabBeranda.setSelected(true);
        binding.nav.tabFavorit.setOnClickListener(v -> buka(R.id.k16_favorit, null));
        binding.nav.tabPesanan.setOnClickListener(v -> buka(R.id.k15_pesanan, null));
        binding.nav.tabProfil.setOnClickListener(v -> buka(R.id.k18_profil, null));

        vm.muat();
        vm.area.observe(
                getViewLifecycleOwner(),
                a ->
                        binding.area.setText(
                                a == null || a.trim().isEmpty()
                                        ? getString(R.string.k07_atur_lokasi)
                                        : a));
        vm.inisial.observe(getViewLifecycleOwner(), binding.avatar::setText);
        vm.urutJarak.observe(
                getViewLifecycleOwner(),
                urut ->
                        binding.judulTerdekat.setText(
                                urut ? R.string.k07_terdekat : R.string.k07_semua_jualan));
        vm.adaNotifikasi.observe(
                getViewLifecycleOwner(),
                ada -> {
                    binding.titikNotifikasi.setVisibility(ada ? View.VISIBLE : View.GONE);
                    binding.tombolNotifikasi.setContentDescription(
                            getString(
                                    ada ? R.string.k07_notifikasi_baru : R.string.k07_notifikasi));
                });
        vm.segeraTutup.observe(
                getViewLifecycleOwner(),
                daftar -> {
                    adapterSegera.submitList(daftar);
                    binding.bagianSegera.setVisibility(daftar.isEmpty() ? View.GONE : View.VISIBLE);
                });
        vm.terdekat.observe(getViewLifecycleOwner(), adapterTerdekat::submitList);
        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanKeadaan());
        vm.terdekat.observe(getViewLifecycleOwner(), d -> tampilkanKeadaan());
        vm.kategori.observe(getViewLifecycleOwner(), this::tandaiKategori);
        vm.filter.observe(
                getViewLifecycleOwner(),
                f -> {
                    tampilkanPenanda(f);
                    tampilkanKeadaan();
                });
        vm.sesiBerakhir.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        ((MainActivity) requireActivity()).sesiBerakhir();
                    }
                });
    }

    private void pasangKategori(
            IncludeUbinKategoriBinding ubin,
            String kode,
            @DrawableRes int ikon,
            @StringRes int label) {
        ubin.ikon.setImageResource(ikon);
        ubin.label.setText(label);
        ubin.getRoot().setTag(kode);
        ubin.getRoot().setOnClickListener(v -> vm.pilihKategori(kode));
    }

    private void tandaiKategori(@Nullable String terpilih) {
        for (IncludeUbinKategoriBinding u :
                new IncludeUbinKategoriBinding[] {
                    binding.kategoriBakery,
                    binding.kategoriKafe,
                    binding.kategoriRestoran,
                    binding.kategoriKatering,
                    binding.kategoriSwalayan
                }) {
            u.getRoot().setSelected(u.getRoot().getTag().equals(terpilih));
        }
        tampilkanKeadaan();
    }

    private void tampilkanKeadaan() {
        BerandaViewModel.Status s = vm.status.getValue();
        List<ListingDto> daftar = vm.terdekat.getValue();
        boolean kosong = daftar == null || daftar.isEmpty();
        boolean memuat = s == BerandaViewModel.Status.MEMUAT;
        boolean gagal = s == BerandaViewModel.Status.GAGAL;

        binding.progres.setVisibility(memuat && kosong ? View.VISIBLE : View.GONE);
        binding.daftarTerdekat.setVisibility(gagal ? View.GONE : View.VISIBLE);
        boolean tampilKeadaan = gagal || (s == BerandaViewModel.Status.SIAP && kosong);
        binding.keadaan.setVisibility(tampilKeadaan ? View.VISIBLE : View.GONE);
        binding.tombolCobaLagi.setVisibility(gagal ? View.VISIBLE : View.GONE);
        FilterJualan f = vm.filter.getValue();
        boolean kosongKarenaFilter =
                !gagal
                        && tampilKeadaan
                        && f != null
                        && (f.alergiAktif() || f.jumlahTambahan(vm.bawaanProfil()) > 0);
        binding.tombolUbahFilter.setVisibility(kosongKarenaFilter ? View.VISIBLE : View.GONE);
        if (gagal) {
            binding.keadaanJudul.setText(R.string.k07_gagal_judul);
            binding.keadaanIsi.setText(R.string.k07_gagal_isi);
        } else if (kosongKarenaFilter) {
            // PRD-04: "Tidak ada jualan tanpa kacang tanah di sekitar Anda saat ini".
            binding.keadaanJudul.setText(R.string.k07_kosong_filter_judul);
            binding.keadaanIsi.setText(
                    f.alergiAktif()
                            ? getString(R.string.k07_kosong_filter_alergi, f.daftarNamaAlergen())
                            : getString(R.string.k07_kosong_filter_isi));
            binding.tombolUbahFilter.setText(
                    f.radiusKm != null ? R.string.k07_perluas_jarak : R.string.k07_atur_filter);
        } else if (tampilKeadaan) {
            binding.keadaanJudul.setText(R.string.k07_kosong_judul);
            binding.keadaanIsi.setText(
                    vm.kategori.getValue() == null
                            ? R.string.k07_kosong_isi
                            : R.string.k07_kosong_kategori);
        }
    }

    /**
     * Penanda di atas kategori selama ada filter yang menyembunyikan jualan (PRD-04 kriteria 5).
     * Ketuk untuk membuka K09.
     */
    private void tampilkanPenanda(@Nullable FilterJualan f) {
        if (f == null) {
            binding.penandaFilter.setVisibility(View.GONE);
            return;
        }
        // Alergen dihitung terpisah, jadi tambahan di sini hanya filter selain alergen.
        FilterJualan tanpaAlergen = f.salin();
        tanpaAlergen.alergen.clear();
        tanpaAlergen.sembunyikanAlergi = true;
        int lain = tanpaAlergen.jumlahTambahan(null);
        String teks;
        if (f.alergiAktif() && lain > 0) {
            teks = getString(R.string.k07_filter_alergi_lain, f.daftarNamaAlergen(), lain);
        } else if (f.alergiAktif()) {
            teks = getString(R.string.k07_filter_alergi, f.daftarNamaAlergen());
        } else if (lain > 0) {
            teks = getString(R.string.k07_filter_lain, lain);
        } else {
            teks = null;
        }
        binding.penandaFilter.setVisibility(teks == null ? View.GONE : View.VISIBLE);
        if (teks != null) {
            binding.teksPenandaFilter.setText(teks);
            binding.penandaFilter.setContentDescription(
                    teks + ". " + getString(R.string.k07_filter_ubah));
        }
    }

    private void bukaFilter() {
        FilterJualan f = vm.filter.getValue();
        buka(R.id.k09_filter, FilterBundle.ke(f == null ? new FilterJualan() : f));
    }

    private void isiKartuFlash(ItemKartuFlashBinding b, ListingDto l) {
        b.judul.setText(l.title);
        b.toko.setText(l.store == null ? "" : l.store.name);
        b.sisaWaktu.setText(FormatTampilan.sisaWaktu(l.minutesUntilEnd));
        isiHarga(b.harga, b.hargaNormal, l);
        b.getRoot().setOnClickListener(v -> bukaDetail(l));
    }

    private void isiKartuTas(ItemKartuTasBinding b, ListingDto l) {
        String jarak = FormatTampilan.jarak(l.distanceKm);
        b.jarak.setText(jarak);
        b.jarak.setVisibility(jarak == null ? View.GONE : View.VISIBLE);
        b.judul.setText(l.title);
        b.toko.setText(l.store == null ? "" : l.store.name);
        b.jamAmbil.setText(FormatTampilan.rentangJam(l.pickupStart, l.pickupEnd));
        isiHarga(b.harga, b.hargaNormal, l);
        b.getRoot().setOnClickListener(v -> bukaDetail(l));
    }

    private static void isiHarga(TextView harga, TextView normal, ListingDto l) {
        harga.setText(FormatTampilan.rupiah(l.priceRupiah));
        Long asli = l.originalValueRupiah;
        if (asli != null && asli > l.priceRupiah) {
            normal.setText(FormatTampilan.rupiah(asli));
            normal.setPaintFlags(normal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            normal.setVisibility(View.VISIBLE);
        } else {
            normal.setVisibility(View.GONE);
        }
    }

    private void bukaDetail(ListingDto l) {
        Bundle args = new Bundle();
        args.putLong(ARG_LISTING_ID, l.id);
        buka(
                ListingDto.TIPE_MENU.equals(l.type) ? R.id.k11_detail_menu : R.id.k10_detail_tas,
                args);
    }

    private void buka(int tujuan, @Nullable Bundle args) {
        NavHostFragment.findNavController(this).navigate(tujuan, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.daftarSegera.setAdapter(null);
        binding.daftarTerdekat.setAdapter(null);
        binding = null;
    }
}
