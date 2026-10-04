package id.lifeoffoods.ui.mitra;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.RiwayatMitra;
import id.lifeoffoods.data.api.model.SaranProduksiDto;
import id.lifeoffoods.databinding.FragmentSaranProduksiBinding;
import id.lifeoffoods.databinding.ItemSaranProduksiBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** M08 Saran produksi (PRD-14). Dari "Lihat saran produksi" di M07. Hanya pemilik. */
public class SaranProduksiFragment extends Fragment {

    private FragmentSaranProduksiBinding binding;
    private SaranProduksiViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSaranProduksiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(SaranProduksiViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.aksi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.m08_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolCatat.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.m06_catat));
        binding.tombolPakaiSemua.setOnClickListener(v -> vm.pakaiSemua());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.saran.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.pesan.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG)
                                .setAnchorView(binding.aksi)
                                .show();
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
        SaranProduksiViewModel.Status s = vm.status.getValue();
        boolean siap = s == SaranProduksiViewModel.Status.SIAP;
        boolean bukanPemilik = s == SaranProduksiViewModel.Status.BUKAN_PEMILIK;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == SaranProduksiViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == SaranProduksiViewModel.Status.GAGAL || bukanPemilik
                        ? View.VISIBLE
                        : View.GONE);
        binding.keadaan.tombolCobaLagi.setVisibility(bukanPemilik ? View.GONE : View.VISIBLE);
        if (bukanPemilik) {
            binding.keadaan.gagalIsi.setText(R.string.m08_bukan_pemilik);
        } else if (s == SaranProduksiViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }

        SaranProduksiDto d = vm.saran.getValue();
        binding.aksi.setVisibility(View.GONE);
        if (!siap || d == null) {
            return;
        }
        String hari = d.weekday == null ? "" : d.weekday;
        binding.kepala.subjudul.setVisibility(View.VISIBLE);
        binding.kepala.subjudul.setText(getString(R.string.m08_untuk, tanggal(d)));
        binding.bannerJudul.setText(getString(R.string.m08_banner_judul, hari));
        binding.bannerIsi.setText(getString(R.string.m08_banner_isi, d.sampleDays, hari));

        boolean ada = d.items != null && !d.items.isEmpty();
        binding.pesan.setVisibility(ada ? View.GONE : View.VISIBLE);
        binding.tombolCatat.setVisibility(d.hasEnoughData ? View.GONE : View.VISIBLE);
        if (!d.hasEnoughData) {
            binding.pesan.setText(getString(R.string.m08_kurang_data, hari, d.sampleDays));
        } else if (!ada) {
            binding.pesan.setText(getString(R.string.m08_tanpa_saran, hari));
        }
        binding.tanpaProduksi.setVisibility(
                d.productsMissingProductionQty > 0 ? View.VISIBLE : View.GONE);
        binding.tanpaProduksi.setText(
                getString(R.string.m08_tanpa_produksi, d.productsMissingProductionQty));

        binding.daftar.removeAllViews();
        if (ada) {
            for (SaranProduksiDto.Butir b : d.items) {
                binding.daftar.addView(kartu(b));
            }
            binding.aksi.setVisibility(View.VISIBLE);
            binding.totalHemat.setText(FormatTampilan.rupiah(d.totalSavingPerWeekRupiah));
            boolean adaBaru = false;
            for (SaranProduksiDto.Butir b : d.items) {
                adaBaru |= SaranProduksiDto.BARU.equals(b.status);
            }
            binding.tombolPakaiSemua.setEnabled(adaBaru);
        }
    }

    private View kartu(SaranProduksiDto.Butir b) {
        ItemSaranProduksiBinding k =
                ItemSaranProduksiBinding.inflate(getLayoutInflater(), binding.daftar, false);
        String satuan =
                b.unit == null || b.unit.isEmpty() ? getString(R.string.m08_satuan_bawaan) : b.unit;
        k.nama.setText(b.name);
        k.kurangi.setText(getString(R.string.m08_kurangi, b.reduceBy, satuan));
        k.biasa.setText(getString(R.string.m08_biasa, b.currentProduction, angka(b.avgWasteQty)));
        k.angkaLama.setText(String.valueOf(b.currentProduction));
        k.angkaLama.setPaintFlags(k.angkaLama.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        k.angkaLama.setContentDescription(getString(R.string.m08_angka_lama, b.currentProduction));
        k.angkaBaru.setText(String.valueOf(b.suggestedProduction));
        k.angkaBaru.setContentDescription(
                getString(R.string.m08_angka_baru, b.suggestedProduction));
        k.satuan.setText(satuan);
        k.hemat.setText(
                getString(
                        R.string.m08_hemat, FormatTampilan.rupiah(b.estimatedSavingPerWeekRupiah)));

        boolean dipakai = SaranProduksiDto.DIPAKAI.equals(b.status);
        boolean diabaikan = SaranProduksiDto.DIABAIKAN.equals(b.status);
        gayaTombol(k.tombolPakai, dipakai);
        // Figma: label tetap "Pakai saran", status dipakai ditandai centang dan latar hijau.
        k.tombolPakai.setContentDescription(
                dipakai ? getString(R.string.m08_dipakai) : getString(R.string.m08_pakai));
        k.tombolAbaikan.setText(diabaikan ? R.string.m08_diabaikan : R.string.m08_abaikan);
        k.tombolPakai.setOnClickListener(v -> vm.tanggapi(b, true));
        k.tombolAbaikan.setOnClickListener(v -> vm.tanggapi(b, false));
        // Saran yang diabaikan diredupkan, tapi tetap bisa dipakai lagi.
        k.getRoot().setAlpha(diabaikan ? 0.6f : 1f);
        return k.getRoot();
    }

    /** "Pakai saran" yang sudah dipakai: latar hijau lembut dan centang, seperti Figma. */
    private void gayaTombol(MaterialButton t, boolean dipakai) {
        int garis =
                ContextCompat.getColor(
                        requireContext(), dipakai ? R.color.merek_utama : R.color.garis_tegas);
        t.setStrokeColor(ColorStateList.valueOf(garis));
        t.setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                requireContext(),
                                dipakai ? R.color.latar_merek_lembut : R.color.latar_kartu)));
        t.setIconResource(dipakai ? R.drawable.ic_centang : 0);
        t.setIconTint(
                ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.teks_merek)));
    }

    /** 6.0 jadi "6", 5.5 jadi "5,5". */
    private static String angka(double nilai) {
        if (nilai == Math.rint(nilai)) {
            return String.valueOf((long) nilai);
        }
        return String.format(Locale.ROOT, "%.1f", nilai).replace('.', ',');
    }

    private static String tanggal(SaranProduksiDto d) {
        try {
            return RiwayatMitra.hariTanggal(LocalDate.parse(d.suggestedForDate));
        } catch (DateTimeParseException | NullPointerException e) {
            return d.weekday == null ? "" : d.weekday;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
