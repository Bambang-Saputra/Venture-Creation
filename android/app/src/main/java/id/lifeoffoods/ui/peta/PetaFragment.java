package id.lifeoffoods.ui.peta;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.BuildConfig;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.TautanPeta;
import id.lifeoffoods.data.TitikToko;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.databinding.FragmentPetaBinding;
import id.lifeoffoods.databinding.ItemKartuPetaBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.beranda.BerandaFragment;
import id.lifeoffoods.ui.umum.FotoJualan;
import id.lifeoffoods.ui.umum.LokasiPerangkat;
import id.lifeoffoods.ui.umum.SisiAman;
import java.io.File;
import java.util.List;
import java.util.Objects;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;

/**
 * K08 Peta (PRD-14). Tab Peta: peta OpenStreetMap lewat osmdroid dengan penanda harga per toko dan
 * kartu jualan terpilih di bawah (ADR-0005, revisi Oktober 2026). Tab Daftar: jualan berurut jarak
 * dengan tombol rute geo:. Dibuka dari tombol peta dan "Lihat semua" di K07.
 */
public class PetaFragment extends Fragment {

    /** Monas, dipakai kalau profil dan semua toko tidak punya koordinat. */
    private static final GeoPoint PUSAT_JAKARTA = new GeoPoint(-6.1754, 106.8272);

    private FragmentPetaBinding binding;
    private PetaViewModel vm;

    /** Radius saat peta terakhir dipusatkan; peta hanya dipusatkan ulang saat radius berubah. */
    private Integer radiusTerpusat;

    private boolean sudahDipusatkan;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        siapkanOsmdroid(requireContext());
        binding = FragmentPetaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Kebijakan ubin OSM mewajibkan User-Agent yang menyebut aplikasi. Cache ubin di folder cache
     * aplikasi, jadi tidak perlu izin penyimpanan.
     */
    private static void siapkanOsmdroid(Context c) {
        IConfigurationProvider k = Configuration.getInstance();
        k.setUserAgentValue(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME);
        File dasar = new File(c.getCacheDir(), "osmdroid");
        k.setOsmdroidBasePath(dasar);
        k.setOsmdroidTileCache(new File(dasar, "ubin"));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PetaViewModel.class);
        vm.muat();
        if (!vm.posisiDiminta && LokasiPerangkat.diizinkan(requireContext())) {
            vm.posisiDiminta = true;
            ambilPosisi(false);
        }

        SisiAman.atas(binding.atas);
        SisiAman.bawah(binding.isi);
        SisiAman.bawah(binding.kartuTerpilih);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.k08_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolMuatLagi.setOnClickListener(v -> vm.muatLagi());
        binding.tabPeta.setOnClickListener(v -> vm.tabPeta.setValue(true));
        binding.tabDaftar.setOnClickListener(v -> vm.tabPeta.setValue(false));

        binding.peta.setTileSource(TileSourceFactory.MAPNIK);
        binding.peta.setMultiTouchControls(true);
        binding.peta.setTilesScaledToDpi(true);
        binding.peta.setMinZoomLevel(11.0);
        binding.peta.setMaxZoomLevel(19.0);
        binding.peta
                .getZoomController()
                .setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        binding.tombolLokasiku.setOnClickListener(v -> keLokasiku());

        List<Integer> pilihan = FilterJualan.PILIHAN_JARAK;
        binding.jarak1.setText(getString(R.string.k08_km, pilihan.get(0)));
        binding.jarak3.setText(getString(R.string.k08_km, pilihan.get(1)));
        binding.jarak5.setText(getString(R.string.k08_km, pilihan.get(2)));
        binding.jarakSemua.setOnClickListener(v -> vm.pilihRadius(null));
        binding.jarak1.setOnClickListener(v -> vm.pilihRadius(pilihan.get(0)));
        binding.jarak3.setOnClickListener(v -> vm.pilihRadius(pilihan.get(1)));
        binding.jarak5.setOnClickListener(v -> vm.pilihRadius(pilihan.get(2)));
        binding.tombolBukaPeta.setOnClickListener(
                v -> bukaPeta(TautanPeta.sekitar(vm.lat(), vm.lng(), vm.area.getValue())));

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.daftar.observe(getViewLifecycleOwner(), d -> tampilkan());
        vm.adaBerikutnya.observe(getViewLifecycleOwner(), a -> tampilkan());
        vm.adaLokasi.observe(getViewLifecycleOwner(), a -> tampilkan());
        vm.tabPeta.observe(getViewLifecycleOwner(), t -> tampilkan());
        vm.area.observe(
                getViewLifecycleOwner(),
                a -> {
                    boolean ada = a != null && !a.isEmpty();
                    binding.kepala.subjudul.setVisibility(ada ? View.VISIBLE : View.GONE);
                    binding.kepala.subjudul.setText(a);
                });
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG).show();
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
    public void onResume() {
        super.onResume();
        binding.peta.onResume();
    }

    /** Sekali per layar: kalau izin sudah ada (diberi di K06), pakai posisi HP. */
    private void ambilPosisi(boolean animasi) {
        LokasiPerangkat.ambil(
                requireContext(),
                l -> {
                    if (binding == null || l == null) {
                        if (binding != null && animasi) {
                            Snackbar.make(
                                            binding.getRoot(),
                                            R.string.k08_posisi_gagal,
                                            Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        return;
                    }
                    sudahDipusatkan = false;
                    vm.pakaiPosisiPerangkat(l.getLatitude(), l.getLongitude());
                });
    }

    /** Tombol lokasiku: minta izin kalau belum, lalu ambil posisi HP dan pusatkan peta. */
    private void keLokasiku() {
        if (LokasiPerangkat.diizinkan(requireContext())) {
            pusatkan(true);
            ambilPosisi(true);
        } else {
            mintaIzin.launch(LokasiPerangkat.IZIN);
        }
    }

    private final ActivityResultLauncher<String[]> mintaIzin =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    hasil -> {
                        if (binding != null && LokasiPerangkat.diizinkan(requireContext())) {
                            ambilPosisi(true);
                        }
                    });

    @Override
    public void onPause() {
        binding.peta.onPause();
        super.onPause();
    }

    private void tampilkan() {
        if (binding == null) {
            return;
        }
        boolean peta = !Boolean.FALSE.equals(vm.tabPeta.getValue());
        gayaTab(binding.tabPeta, peta);
        gayaTab(binding.tabDaftar, !peta);

        PetaViewModel.Status s = vm.status.getValue();
        boolean siap = s == PetaViewModel.Status.SIAP;
        boolean gagal = s == PetaViewModel.Status.GAGAL;
        binding.panelPeta.setVisibility(peta ? View.VISIBLE : View.GONE);
        binding.gulir.setVisibility(peta ? View.GONE : View.VISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(siap || gagal ? View.GONE : View.VISIBLE);
        binding.keadaan.gagal.setVisibility(gagal ? View.VISIBLE : View.GONE);
        if (gagal && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }

        boolean lokasi = Boolean.TRUE.equals(vm.adaLokasi.getValue());
        // Tanpa koordinat, radius tidak berarti: server mengabaikannya.
        binding.jarak.setVisibility(lokasi ? View.VISIBLE : View.GONE);
        binding.tanpaLokasi.setVisibility(siap && !lokasi ? View.VISIBLE : View.GONE);
        binding.tombolLokasiku.setVisibility(View.VISIBLE);
        Integer r = vm.radius.getValue();
        binding.jarakSemua.setChecked(r == null);
        binding.jarak1.setChecked(r != null && r.equals(FilterJualan.PILIHAN_JARAK.get(0)));
        binding.jarak3.setChecked(r != null && r.equals(FilterJualan.PILIHAN_JARAK.get(1)));
        binding.jarak5.setChecked(r != null && r.equals(FilterJualan.PILIHAN_JARAK.get(2)));

        List<ListingDto> d = siap ? vm.daftar.getValue() : null;
        boolean kosong = d == null || d.isEmpty();
        binding.kosong.setVisibility(siap && kosong ? View.VISIBLE : View.GONE);
        binding.tombolMuatLagi.setVisibility(
                siap && Boolean.TRUE.equals(vm.adaBerikutnya.getValue())
                        ? View.VISIBLE
                        : View.GONE);
        binding.daftar.removeAllViews();
        if (!kosong) {
            for (ListingDto l : d) {
                binding.daftar.addView(kartu(l, binding.daftar));
            }
        }
        if (siap) {
            gambarPeta(TitikToko.dari(d));
        }
    }

    private void gayaTab(TextView tv, boolean terpilih) {
        tv.setSelected(terpilih);
        tv.setBackgroundResource(terpilih ? R.drawable.bg_tab_aktif : 0);
        tv.setTextColor(
                ContextCompat.getColor(
                        requireContext(), terpilih ? R.color.teks_merek : R.color.teks_sekunder));
    }

    /** Penanda lokasi pembeli, penanda harga per toko, lalu kartu toko terpilih. */
    private void gambarPeta(List<TitikToko> titik) {
        binding.peta.getOverlays().clear();
        TitikToko pilih = null;
        for (TitikToko t : titik) {
            if (t.jualan.store.id == vm.tokoTerpilih) {
                pilih = t;
            }
        }
        if (pilih == null && !titik.isEmpty()) {
            pilih = titik.get(0);
            vm.tokoTerpilih = pilih.jualan.store.id;
        }

        Double lat = vm.lat();
        Double lng = vm.lng();
        if (lat != null && lng != null) {
            Marker aku = new Marker(binding.peta);
            aku.setPosition(new GeoPoint(lat, lng));
            Drawable pin = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pin_lokasi);
            if (pin != null) {
                pin = DrawableCompat.wrap(pin.mutate());
                DrawableCompat.setTint(
                        pin, ContextCompat.getColor(requireContext(), R.color.merek_utama));
            }
            aku.setIcon(pin);
            aku.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            aku.setInfoWindow(null);
            aku.setTitle(getString(R.string.k08_lokasiku));
            binding.peta.getOverlays().add(aku);
        }

        // Yang terpilih ditambahkan terakhir supaya tergambar di atas penanda lain.
        for (TitikToko t : titik) {
            if (t != pilih) {
                binding.peta.getOverlays().add(penanda(t, false));
            }
        }
        if (pilih != null) {
            binding.peta.getOverlays().add(penanda(pilih, true));
        }
        binding.peta.invalidate();

        binding.kartuTerpilih.removeAllViews();
        binding.kartuTerpilih.setVisibility(pilih == null ? View.GONE : View.VISIBLE);
        if (pilih != null) {
            binding.kartuTerpilih.addView(kartu(pilih.jualan, binding.kartuTerpilih));
        }

        if (!sudahDipusatkan || !Objects.equals(radiusTerpusat, vm.radius.getValue())) {
            pusatkan(false, titik);
        }
    }

    private Marker penanda(TitikToko t, boolean terpilih) {
        String harga = FormatTampilan.rupiah(t.jualan.priceRupiah);
        String teks = t.jumlah > 1 ? getString(R.string.k08_penanda_lebih, harga, t.jumlah) : harga;
        Marker m = new Marker(binding.peta);
        m.setPosition(new GeoPoint(t.lat(), t.lng()));
        m.setIcon(PenandaHarga.gambar(requireContext(), teks, terpilih));
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        m.setInfoWindow(null);
        m.setTitle(getString(R.string.k08_penanda, t.jualan.store.name, teks));
        m.setOnMarkerClickListener(
                (marker, peta) -> {
                    vm.tokoTerpilih = t.jualan.store.id;
                    tampilkan();
                    return true;
                });
        return m;
    }

    private void pusatkan(boolean animasi) {
        pusatkan(animasi, TitikToko.dari(vm.daftar.getValue()));
    }

    /** Ke lokasi pembeli; tanpa itu ke toko terdekat; tanpa itu ke pusat Jakarta. */
    private void pusatkan(boolean animasi, List<TitikToko> titik) {
        Double lat = vm.lat();
        Double lng = vm.lng();
        GeoPoint pusat;
        if (lat != null && lng != null) {
            pusat = new GeoPoint(lat, lng);
        } else if (!titik.isEmpty()) {
            pusat = new GeoPoint(titik.get(0).lat(), titik.get(0).lng());
        } else {
            pusat = PUSAT_JAKARTA;
        }
        radiusTerpusat = vm.radius.getValue();
        sudahDipusatkan = true;
        double zoom = TitikToko.zoomUntuk(radiusTerpusat);
        if (animasi) {
            binding.peta.getController().animateTo(pusat, zoom, 400L);
        } else {
            binding.peta.getController().setZoom(zoom);
            binding.peta.getController().setCenter(pusat);
        }
    }

    private View kartu(ListingDto l, ViewGroup induk) {
        ItemKartuPetaBinding b = ItemKartuPetaBinding.inflate(getLayoutInflater(), induk, false);
        FotoJualan.muat(b.fotoJualan, l.photoUrl, 14);
        String jarak = FormatTampilan.jarak(l.distanceKm);
        String tutup = FormatTampilan.jam(l.pickupEnd);
        b.meta.setText(
                jarak == null
                        ? getString(R.string.k08_meta_tanpa_jarak, tutup)
                        : getString(R.string.k08_meta, jarak, tutup));
        b.judul.setText(l.title);
        String toko = l.store == null ? "" : l.store.name;
        b.toko.setText(toko);
        b.harga.setText(FormatTampilan.rupiah(l.priceRupiah));
        Long asli = l.originalValueRupiah;
        boolean coret = asli != null && asli > l.priceRupiah;
        b.hargaNormal.setVisibility(coret ? View.VISIBLE : View.GONE);
        if (coret) {
            b.hargaNormal.setText(FormatTampilan.rupiah(asli));
            b.hargaNormal.setPaintFlags(
                    b.hargaNormal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        String uri =
                l.store == null
                        ? null
                        : TautanPeta.toko(
                                l.store.latitude, l.store.longitude, l.store.name, l.store.address);
        b.tombolRute.setVisibility(uri == null ? View.GONE : View.VISIBLE);
        b.tombolRute.setContentDescription(getString(R.string.k08_rute, toko));
        b.tombolRute.setOnClickListener(v -> bukaPeta(uri));
        b.getRoot().setOnClickListener(v -> bukaDetail(l));
        return b.getRoot();
    }

    private void bukaDetail(ListingDto l) {
        Bundle args = new Bundle();
        args.putLong(BerandaFragment.ARG_LISTING_ID, l.id);
        NavHostFragment.findNavController(this)
                .navigate(
                        ListingDto.TIPE_MENU.equals(l.type)
                                ? R.id.k11_detail_menu
                                : R.id.k10_detail_tas,
                        args);
    }

    private void bukaPeta(@Nullable String uri) {
        if (uri == null) {
            Snackbar.make(binding.getRoot(), R.string.k08_tanpa_lokasi, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), R.string.k08_peta_tidak_ada, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.peta.onDetach();
        binding = null;
    }
}
