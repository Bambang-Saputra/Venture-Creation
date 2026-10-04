package id.lifeoffoods.ui.favorit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.StatusFavorit;
import id.lifeoffoods.data.api.model.FavoritDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.databinding.FragmentFavoritBinding;
import id.lifeoffoods.databinding.ItemKartuFavoritBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.beranda.BerandaFragment;
import id.lifeoffoods.ui.mitra.ProfilTokoFragment;
import id.lifeoffoods.ui.umum.FotoJualan;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;

/**
 * K16 Favorit (PRD-22), tab Mitra. Ketuk kartu membuka jualan toko itu hari ini; ikon hati
 * menghapusnya dari favorit (bisa dibatalkan dari Snackbar).
 */
public class FavoritFragment extends Fragment {

    private FragmentFavoritBinding binding;
    private FavoritViewModel vm;
    private boolean mengisi;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(FavoritViewModel.class);

        SisiAman.atas(binding.header);
        SisiAman.bawah(binding.nav.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);

        binding.kabari.judul.setText(R.string.k16_kabari);
        binding.kabari.ket.setText(R.string.k16_kabari_ket);
        binding.kabari.sakelar.setContentDescription(getString(R.string.k16_kabari));
        binding.kabari.sakelar.setOnCheckedChangeListener(
                (b, nyala) -> {
                    if (!mengisi) {
                        vm.ubahKabari(nyala);
                    }
                });
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muat());
        binding.tombolBeranda.setOnClickListener(v -> keBeranda());

        binding.nav.tabFavorit.setSelected(true);
        binding.nav.tabBeranda.setOnClickListener(v -> keBeranda());
        binding.nav.tabPesanan.setOnClickListener(v -> buka(R.id.k15_pesanan));
        binding.nav.tabProfil.setOnClickListener(v -> buka(R.id.k18_profil));

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.daftar.observe(getViewLifecycleOwner(), d -> tampilkan());
        vm.kabari.observe(
                getViewLifecycleOwner(),
                k -> {
                    mengisi = true;
                    binding.kabari.sakelar.setChecked(Boolean.TRUE.equals(k));
                    mengisi = false;
                });
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.terhapus.observe(
                getViewLifecycleOwner(),
                p -> {
                    FavoritDto f = p.ambil();
                    if (f != null) {
                        Snackbar.make(
                                        binding.getRoot(),
                                        getString(R.string.k16_terhapus, f.name),
                                        Snackbar.LENGTH_LONG)
                                .setAction(R.string.k16_batalkan, v -> vm.batalHapus(f))
                                .show();
                    }
                });
        vm.bukaJualan.observe(
                getViewLifecycleOwner(),
                p -> {
                    ListingDto l = p.ambil();
                    if (l != null) {
                        Bundle a = new Bundle();
                        a.putLong(BerandaFragment.ARG_LISTING_ID, l.id);
                        NavHostFragment.findNavController(this)
                                .navigate(
                                        ListingDto.TIPE_MENU.equals(l.type)
                                                ? R.id.k11_detail_menu
                                                : R.id.k10_detail_tas,
                                        a);
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
        // Hati bisa diubah di K10/K11 selama layar ini tertutup.
        vm.muat();
    }

    private void tampilkan() {
        if (binding == null) {
            return;
        }
        FavoritViewModel.Status s = vm.status.getValue();
        boolean siap = s == FavoritViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == FavoritViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == FavoritViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == FavoritViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }

        List<FavoritDto> d = vm.daftar.getValue();
        boolean kosong = d == null || d.isEmpty();
        binding.kosong.setVisibility(siap && kosong ? View.VISIBLE : View.GONE);
        binding.daftar.removeAllViews();
        if (!kosong) {
            for (FavoritDto f : d) {
                binding.daftar.addView(kartu(f));
            }
        }
    }

    private View kartu(FavoritDto f) {
        ItemKartuFavoritBinding b =
                ItemKartuFavoritBinding.inflate(getLayoutInflater(), binding.daftar, false);
        b.ikonKategori.setImageResource(ProfilTokoFragment.ikonKategori(f.category));
        FotoJualan.muat(b.fotoJualan, f.photoUrl, 14);
        b.nama.setText(f.name);

        String meta = getString(ProfilTokoFragment.namaKategori(f.category));
        String jarak = FormatTampilan.jarak(f.distanceKm);
        if (jarak != null) {
            meta = getString(R.string.k16_meta, meta, jarak);
        }
        String tutup =
                f.closesAt == null
                        ? getString(R.string.k16_tutup_hari_ini)
                        : getString(R.string.k16_tutup_jam, f.closesAt.replace(':', '.'));
        b.meta.setText(getString(R.string.k16_meta, meta, tutup));

        StatusFavorit st = StatusFavorit.dari(f);
        switch (st.jenis) {
            case TAS:
                b.status.setText(
                        getResources()
                                .getQuantityString(
                                        R.plurals.k16_tas_tersedia, st.jumlah, st.jumlah));
                break;
            case MENU:
                b.status.setText(R.string.k16_menu_tersedia);
                break;
            case TUTUP:
                b.status.setText(R.string.k16_tutup_sementara);
                break;
            case BIASANYA:
                b.status.setText(getString(R.string.k16_biasanya, st.jam));
                break;
            default:
                b.status.setText(R.string.k16_belum_ada_tas);
                break;
        }
        b.status.setBackgroundResource(
                st.tersedia() ? R.drawable.bg_pil_merek : R.drawable.bg_pil_isian);
        b.status.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                        requireContext(),
                        st.tersedia() ? R.color.teks_merek_gelap : R.color.teks_kuat));

        b.tombolHati.setContentDescription(getString(R.string.k16_hapus, f.name));
        b.tombolHati.setOnClickListener(v -> vm.hapus(f));
        b.getRoot().setOnClickListener(v -> vm.buka(f));
        return b.getRoot();
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
        binding = null;
    }
}
