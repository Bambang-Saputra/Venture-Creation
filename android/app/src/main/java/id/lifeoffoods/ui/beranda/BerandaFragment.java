package id.lifeoffoods.ui.beranda;

import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
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
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.PagerSnapHelper;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.BannerDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.databinding.FragmentBerandaBinding;
import id.lifeoffoods.databinding.IncludeUbinKategoriBinding;
import id.lifeoffoods.databinding.ItemBannerPromosiBinding;
import id.lifeoffoods.databinding.ItemKartuFlashBinding;
import id.lifeoffoods.databinding.ItemKartuTasBinding;
import id.lifeoffoods.databinding.ItemKartuWarBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.filter.FilterBundle;
import id.lifeoffoods.ui.jualan.BeliLangsung;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.FotoJualan;
import id.lifeoffoods.ui.umum.HargaDiFoto;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * K07 Beranda konsumen. Kolom cari membuka K09 Filter; hasilnya kembali lewat setFragmentResult.
 * Tujuan lain (K08 peta, K16-K18) masih penanda di nav_konsumen sampai layarnya dibuat.
 */
public class BerandaFragment extends Fragment {

    public static final String ARG_LISTING_ID = "listing_id";

    private FragmentBerandaBinding binding;
    private BerandaViewModel vm;

    /** Jumlah kartu di WAR JAM INI. */
    private static final int MAKS_WAR = 6;

    private final Handler detak = new Handler(Looper.getMainLooper());
    @Nullable private OffsetDateTime akhirWar;
    private BaseListAdapter<ListingDto, ItemKartuWarBinding> adapterWar;

    /** Hitung mundur WAR JAM INI, berdetik selama Beranda tampil. */
    private final Runnable hitungMundur =
            new Runnable() {
                @Override
                public void run() {
                    tampilkanHitungMundur();
                    detak.postDelayed(this, 1000);
                }
            };

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

        BaseListAdapter<BannerDto, ItemBannerPromosiBinding> adapterBanner =
                new BaseListAdapter<>(
                        ItemBannerPromosiBinding::inflate, this::isiBanner, b -> b.storeId);
        binding.daftarBanner.setAdapter(adapterBanner);
        new PagerSnapHelper().attachToRecyclerView(binding.daftarBanner);
        BaseListAdapter<ListingDto, ItemKartuFlashBinding> adapterPopuler =
                new BaseListAdapter<>(
                        ItemKartuFlashBinding::inflate, this::isiKartuPopuler, l -> l.id);
        binding.daftarPopuler.setAdapter(adapterPopuler);
        // Digeser satu kartu per sapuan, seperti carousel Flash Sale.
        new LinearSnapHelper().attachToRecyclerView(binding.daftarPopuler);
        BaseListAdapter<ListingDto, ItemKartuFlashBinding> adapterSegera =
                new BaseListAdapter<>(
                        ItemKartuFlashBinding::inflate, this::isiKartuFlash, l -> l.id);
        binding.daftarSegera.setAdapter(adapterSegera);
        adapterWar =
                new BaseListAdapter<>(ItemKartuWarBinding::inflate, this::isiKartuWar, l -> l.id);
        binding.daftarWar.setAdapter(adapterWar);
        new LinearSnapHelper().attachToRecyclerView(binding.daftarWar);

        // Tarik ke bawah untuk memuat ulang, seperti aplikasi lain.
        binding.segarkan.setColorSchemeResources(R.color.merek_utama, R.color.war_aksen);
        binding.segarkan.setOnRefreshListener(() -> vm.muatUlang());
        pasangHeaderMengecil();

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
        vm.banner.observe(
                getViewLifecycleOwner(),
                daftar -> {
                    adapterBanner.submitList(daftar);
                    binding.daftarBanner.setVisibility(daftar.isEmpty() ? View.GONE : View.VISIBLE);
                });
        vm.populer.observe(
                getViewLifecycleOwner(),
                daftar -> {
                    adapterPopuler.submitList(daftar);
                    binding.bagianPopuler.setVisibility(
                            daftar.isEmpty() ? View.GONE : View.VISIBLE);
                    binding.geserPopuler.setVisibility(
                            daftar.size() > 1 ? View.VISIBLE : View.GONE);
                });
        vm.segeraTutup.observe(
                getViewLifecycleOwner(),
                daftar -> {
                    adapterSegera.submitList(daftar);
                    binding.bagianSegera.setVisibility(daftar.isEmpty() ? View.GONE : View.VISIBLE);
                });
        vm.terdekat.observe(getViewLifecycleOwner(), adapterTerdekat::submitList);
        vm.terdekat.observe(getViewLifecycleOwner(), d -> susunWar());
        vm.segeraTutup.observe(getViewLifecycleOwner(), d -> susunWar());
        vm.populer.observe(getViewLifecycleOwner(), d -> susunWar());
        vm.status.observe(
                getViewLifecycleOwner(),
                s -> {
                    tampilkanKeadaan();
                    if (s != BerandaViewModel.Status.MEMUAT) {
                        binding.segarkan.setRefreshing(false);
                    }
                });
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

    /**
     * Header mengecil: saat isi digulir ke bawah, baris logo/lokasi/notifikasi disembunyikan dan
     * hanya kolom cari yang tersisa; saat digulir ke atas baris itu muncul lagi. Bayangan tipis
     * memberi batas yang jelas antara header dan isi yang lewat di bawahnya.
     */
    private void pasangHeaderMengecil() {
        float bayangan = 6 * getResources().getDisplayMetrics().density;
        int ambang = Math.round(48 * getResources().getDisplayMetrics().density);
        binding.gulir.setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                        (v, x, y, xLama, yLama) -> {
                            binding.header.setElevation(y > 0 ? bayangan : 0f);
                            boolean tampil = binding.barisAtas.getVisibility() == View.VISIBLE;
                            boolean kecilkan = y > ambang && y > yLama;
                            boolean besarkan = y < ambang || yLama - y > ambang / 4;
                            if (tampil && kecilkan) {
                                TransitionManager.beginDelayedTransition(binding.header);
                                binding.barisAtas.setVisibility(View.GONE);
                            } else if (!tampil && besarkan) {
                                TransitionManager.beginDelayedTransition(binding.header);
                                binding.barisAtas.setVisibility(View.VISIBLE);
                            }
                        });
    }

    /**
     * WAR JAM INI: gabungan semua jualan yang sudah dimuat dan masih ada stoknya, diurutkan dari
     * jam ambil yang paling cepat berakhir lalu diskon terbesar.
     */
    private void susunWar() {
        Map<Long, ListingDto> unik = new LinkedHashMap<>();
        List<List<ListingDto>> sumber = new ArrayList<>();
        sumber.add(nilai(vm.segeraTutup.getValue()));
        sumber.add(nilai(vm.populer.getValue()));
        sumber.add(nilai(vm.terdekat.getValue()));
        for (List<ListingDto> d : sumber) {
            for (ListingDto l : d) {
                if (l.qtyRemaining > 0 && l.minutesUntilEnd > 0) {
                    unik.putIfAbsent(l.id, l);
                }
            }
        }
        List<ListingDto> war = new ArrayList<>(unik.values());
        war.sort(
                Comparator.comparingInt((ListingDto l) -> l.minutesUntilEnd)
                        .thenComparing(l -> -HargaDiFoto.diskon(l)));
        if (war.size() > MAKS_WAR) {
            war = new ArrayList<>(war.subList(0, MAKS_WAR));
        }
        adapterWar.submitList(war);
        binding.bagianWar.setVisibility(war.isEmpty() ? View.GONE : View.VISIBLE);
        if (war.isEmpty()) {
            akhirWar = null;
            return;
        }
        int maksDiskon = 0;
        for (ListingDto l : war) {
            maksDiskon = Math.max(maksDiskon, HargaDiFoto.diskon(l));
        }
        String jamAkhir = FormatTampilan.jam(war.get(0).pickupEnd);
        binding.subWar.setText(
                maksDiskon > 0
                        ? getString(R.string.war_sub, maksDiskon, jamAkhir)
                        : getString(R.string.war_sub_tanpa_diskon, jamAkhir));
        akhirWar = waktu(war.get(0).pickupEnd);
        binding.ketHitung.setText(R.string.war_hitung);
        tampilkanHitungMundur();
    }

    private void tampilkanHitungMundur() {
        if (binding == null || akhirWar == null) {
            return;
        }
        long detik = Math.max(0, Duration.between(OffsetDateTime.now(), akhirWar).getSeconds());
        long j = detik / 3600;
        long m = (detik % 3600) / 60;
        binding.hitungJam.setText(String.format(Locale.ROOT, "%02d", j));
        binding.hitungMenit.setText(String.format(Locale.ROOT, "%02d", m));
        binding.hitungDetik.setText(String.format(Locale.ROOT, "%02d", detik % 60));
        binding.barisHitung.setContentDescription(
                getString(R.string.war_hitung_desk, (int) j, (int) m));
    }

    private void isiKartuWar(ItemKartuWarBinding b, ListingDto l) {
        FotoJualan.muat(b.fotoJualan, l.photoUrl, 17, 0);
        HargaDiFoto.isi(b.overlayHarga, l, true);
        b.judul.setText(l.title);
        String toko = l.store == null ? "" : l.store.name;
        String jarak = FormatTampilan.jarak(l.distanceKm);
        b.toko.setText(jarak == null ? toko : toko + " · " + jarak);
        boolean menipis = l.qtyRemaining <= 3;
        b.stok.setText(
                getString(menipis ? R.string.war_sisa : R.string.war_sisa_banyak, l.qtyRemaining));
        b.stok.setBackgroundResource(menipis ? R.drawable.bg_pil_war : R.drawable.bg_pil_lime);
        b.stok.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                        requireContext(), menipis ? R.color.teks_di_gelap : R.color.teks_utama));
        b.getRoot()
                .setContentDescription(
                        getString(
                                R.string.war_kartu_desk,
                                l.title,
                                toko,
                                FormatTampilan.rupiah(l.priceRupiah),
                                l.qtyRemaining));
        b.getRoot().setOnClickListener(v -> bukaDetail(l));
        b.tombolAmbil.setOnClickListener(v -> BeliLangsung.satu(this, l));
    }

    private static List<ListingDto> nilai(@Nullable List<ListingDto> d) {
        return d == null ? new ArrayList<>() : d;
    }

    @Nullable
    private static OffsetDateTime waktu(@Nullable String iso) {
        if (iso == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        detak.post(hitungMundur);
    }

    @Override
    public void onPause() {
        super.onPause();
        detak.removeCallbacks(hitungMundur);
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
        // Sudut atas 17dp mengikuti bg_slot_foto_atas.
        FotoJualan.muat(b.fotoJualan, l.photoUrl, 17, 0);
        b.judul.setText(l.title);
        b.toko.setText(l.store == null ? "" : l.store.name);
        b.sisaWaktu.setText(FormatTampilan.sisaWaktu(l.minutesUntilEnd));
        isiHarga(b.harga, b.hargaNormal, l);
        // Uji coba: harga dan diskon ditaruh di dalam foto, jadi baris harga di bawah
        // disembunyikan.
        HargaDiFoto.isi(b.overlayHarga, l, true);
        b.harga.setVisibility(View.GONE);
        b.hargaNormal.setVisibility(View.GONE);
        b.getRoot().setOnClickListener(v -> bukaDetail(l));
    }

    /** Kartu yang sama dengan "Tutup kurang dari satu jam", tapi pilnya jumlah yang dipesan. */
    private void isiKartuPopuler(ItemKartuFlashBinding b, ListingDto l) {
        isiKartuFlash(b, l);
        b.sisaWaktu.setText(getString(R.string.k07_dipesan, l.qtyOrdered));
        b.sisaWaktu.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
    }

    private void isiBanner(ItemBannerPromosiBinding b, BannerDto d) {
        FotoJualan.muat(b.foto, d.photoUrl, 18);
        b.toko.setText(d.storeName);
        boolean adaKalimat = d.headline != null && !d.headline.trim().isEmpty();
        b.kalimat.setVisibility(adaKalimat ? View.VISIBLE : View.GONE);
        b.kalimat.setText(adaKalimat ? d.headline : "");
        b.mulai.setText(
                getString(R.string.k07_banner_mulai, FormatTampilan.rupiah(d.priceFromRupiah)));
        b.getRoot()
                .setContentDescription(
                        getString(R.string.iklan)
                                + ". "
                                + d.storeName
                                + (adaKalimat ? ". " + d.headline : "")
                                + ". "
                                + b.mulai.getText());
        b.getRoot()
                .setOnClickListener(
                        v -> {
                            Bundle args = new Bundle();
                            args.putLong(ARG_LISTING_ID, d.listingId);
                            buka(
                                    ListingDto.TIPE_MENU.equals(d.listingType)
                                            ? R.id.k11_detail_menu
                                            : R.id.k10_detail_tas,
                                    args);
                        });
    }

    private void isiKartuTas(ItemKartuTasBinding b, ListingDto l) {
        // Sudut 14dp mengikuti bg_slot_foto.
        FotoJualan.muat(b.fotoJualan, l.photoUrl, 14);
        b.labelIklan.setVisibility(l.isSponsored ? View.VISIBLE : View.GONE);
        String jarak = FormatTampilan.jarak(l.distanceKm);
        b.jarak.setText(jarak);
        b.jarak.setVisibility(jarak == null ? View.GONE : View.VISIBLE);
        b.judul.setText(l.title);
        b.toko.setText(l.store == null ? "" : l.store.name);
        b.jamAmbil.setText(FormatTampilan.rentangJam(l.pickupStart, l.pickupEnd));
        isiHarga(b.harga, b.hargaNormal, l);
        // Foto 94dp terlalu kecil untuk harga; cukup pil diskon di pojoknya.
        HargaDiFoto.isi(b.overlayHarga, l, false);
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
        binding.daftarBanner.setAdapter(null);
        binding.daftarPopuler.setAdapter(null);
        binding.daftarSegera.setAdapter(null);
        binding.daftarTerdekat.setAdapter(null);
        binding = null;
    }
}
