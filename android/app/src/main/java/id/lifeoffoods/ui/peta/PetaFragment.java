package id.lifeoffoods.ui.peta;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FilterJualan;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.TautanPeta;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.databinding.FragmentPetaBinding;
import id.lifeoffoods.databinding.ItemKartuPetaBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.beranda.BerandaFragment;
import id.lifeoffoods.ui.umum.FotoJualan;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;

/** K08 Peta versi daftar (PRD-14, ADR-0005). Dari tombol peta dan "Lihat semua" di K07. */
public class PetaFragment extends Fragment {

    private FragmentPetaBinding binding;
    private PetaViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPetaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PetaViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.isi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.k08_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolMuatLagi.setOnClickListener(v -> vm.muatLagi());

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

    private void tampilkan() {
        if (binding == null) {
            return;
        }
        PetaViewModel.Status s = vm.status.getValue();
        boolean siap = s == PetaViewModel.Status.SIAP;
        boolean gagal = s == PetaViewModel.Status.GAGAL;
        binding.gulir.setVisibility(View.VISIBLE);
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
        Integer r = vm.radius.getValue();
        binding.jarakSemua.setChecked(r == null);
        binding.jarak1.setChecked(r != null && r.equals(FilterJualan.PILIHAN_JARAK.get(0)));
        binding.jarak3.setChecked(r != null && r.equals(FilterJualan.PILIHAN_JARAK.get(1)));
        binding.jarak5.setChecked(r != null && r.equals(FilterJualan.PILIHAN_JARAK.get(2)));

        binding.daftar.removeAllViews();
        List<ListingDto> d = siap ? vm.daftar.getValue() : null;
        boolean kosong = d == null || d.isEmpty();
        binding.kosong.setVisibility(siap && kosong ? View.VISIBLE : View.GONE);
        binding.tombolMuatLagi.setVisibility(
                siap && Boolean.TRUE.equals(vm.adaBerikutnya.getValue())
                        ? View.VISIBLE
                        : View.GONE);
        if (kosong) {
            return;
        }
        for (ListingDto l : d) {
            binding.daftar.addView(kartu(l));
        }
    }

    private View kartu(ListingDto l) {
        ItemKartuPetaBinding b =
                ItemKartuPetaBinding.inflate(getLayoutInflater(), binding.daftar, false);
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
        binding = null;
    }
}
