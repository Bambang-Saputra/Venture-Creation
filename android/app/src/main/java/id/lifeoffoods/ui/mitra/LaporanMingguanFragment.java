package id.lifeoffoods.ui.mitra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import id.lifeoffoods.R;
import id.lifeoffoods.data.CatatSisa;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.LaporanMingguan;
import id.lifeoffoods.data.api.model.LaporanMingguanDto;
import id.lifeoffoods.databinding.FragmentLaporanMingguanBinding;
import id.lifeoffoods.databinding.ItemProdukTeratasBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.util.List;

/** M07 Laporan mingguan (PRD-13). Hanya pemilik; kasir melihat keterangan, bukan galat. */
public class LaporanMingguanFragment extends Fragment {

    /** Tinggi batang tertinggi, dp (Figma: 100). */
    private static final int BATANG_PENUH_DP = 100;

    private FragmentLaporanMingguanBinding binding;
    private LaporanMingguanViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLaporanMingguanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(LaporanMingguanViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.isi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.kepala.subjudul.setVisibility(View.VISIBLE);
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolSebelumnya.setOnClickListener(v -> vm.geser(-1));
        binding.tombolBerikutnya.setOnClickListener(v -> vm.geser(1));
        binding.tombolCatatKosong.setOnClickListener(v -> buka(R.id.m06_catat));
        binding.tombolCatatAjakan.setOnClickListener(v -> buka(R.id.m06_catat));
        binding.tombolSaran.setOnClickListener(v -> buka(R.id.m08_saran));

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.laporan.observe(getViewLifecycleOwner(), l -> tampilkan());
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
        LocalDate senin = vm.senin();
        binding.kepala.judul.setText(
                vm.mingguIni() ? R.string.m07_judul_ini : R.string.m07_judul_lalu);
        binding.kepala.subjudul.setText(LaporanMingguan.rentang(senin, senin.plusDays(6)));
        binding.tombolBerikutnya.setVisibility(vm.mingguIni() ? View.INVISIBLE : View.VISIBLE);

        LaporanMingguanViewModel.Status s = vm.status.getValue();
        boolean siap = s == LaporanMingguanViewModel.Status.SIAP;
        boolean bukanPemilik = s == LaporanMingguanViewModel.Status.BUKAN_PEMILIK;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == LaporanMingguanViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == LaporanMingguanViewModel.Status.GAGAL || bukanPemilik
                        ? View.VISIBLE
                        : View.GONE);
        binding.keadaan.tombolCobaLagi.setVisibility(bukanPemilik ? View.GONE : View.VISIBLE);
        if (bukanPemilik) {
            binding.keadaan.gagalIsi.setText(R.string.m07_bukan_pemilik);
        } else if (s == LaporanMingguanViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }

        LaporanMingguanDto l = vm.laporan.getValue();
        if (!siap || l == null) {
            return;
        }
        boolean kosong = l.loggedDays <= 0;
        binding.kosong.setVisibility(kosong ? View.VISIBLE : View.GONE);
        binding.konten.setVisibility(kosong ? View.GONE : View.VISIBLE);
        if (kosong) {
            binding.kosongIsi.setText(
                    vm.mingguIni() ? R.string.m07_kosong : R.string.m07_kosong_lalu);
            binding.tombolCatatKosong.setVisibility(vm.mingguIni() ? View.VISIBLE : View.GONE);
            return;
        }
        isiRingkasan(l);
        isiTeratas(l.topWastedProducts);
        isiGrafik(l);
    }

    private void isiRingkasan(LaporanMingguanDto l) {
        binding.nilaiTidakTerjual.setText(FormatTampilan.rupiah(l.unsoldValueRupiah));
        Integer persen = l.wastedChangePercent;
        binding.tren.setVisibility(persen == null ? View.GONE : View.VISIBLE);
        if (persen != null) {
            boolean naik = persen > 0;
            int warna =
                    ContextCompat.getColor(
                            requireContext(),
                            naik ? R.color.tren_naik_di_gelap : R.color.merek_lime);
            binding.ikonTren.setImageResource(
                    naik ? R.drawable.ic_tren_naik : R.drawable.ic_tren_turun);
            binding.ikonTren.setColorFilter(warna);
            binding.ikonTren.setVisibility(persen == 0 ? View.GONE : View.VISIBLE);
            binding.teksTren.setTextColor(warna);
            binding.teksTren.setText(
                    persen > 0
                            ? getString(R.string.m07_tren_naik, persen)
                            : persen < 0
                                    ? getString(R.string.m07_tren_turun, -persen)
                                    : getString(R.string.m07_tren_sama));
        }
        binding.nilaiTerselamatkan.setText(FormatTampilan.rupiah(l.rescuedValueRupiah));
        binding.subTerselamatkan.setText(
                getResources().getQuantityString(R.plurals.m07_terjual, l.itemsSold, l.itemsSold));
        binding.nilaiTerbuang.setText(FormatTampilan.rupiah(l.wastedValueRupiah));
        binding.subTerbuang.setText(
                l.wastedWeightGram > 0
                        ? getString(R.string.m07_sekitar_berat, CatatSisa.kg(l.wastedWeightGram))
                        : getResources()
                                .getQuantityString(
                                        R.plurals.m07_hari_tercatat, l.loggedDays, l.loggedDays));
    }

    private void isiTeratas(@Nullable List<LaporanMingguanDto.Produk> produk) {
        binding.daftarTeratas.removeAllViews();
        boolean ada = produk != null && !produk.isEmpty();
        binding.kartuTeratas.setVisibility(ada ? View.VISIBLE : View.GONE);
        if (!ada) {
            return;
        }
        long maks = 0;
        for (LaporanMingguanDto.Produk p : produk) {
            maks = Math.max(maks, p.valueRupiah);
        }
        for (LaporanMingguanDto.Produk p : produk) {
            ItemProdukTeratasBinding b =
                    ItemProdukTeratasBinding.inflate(
                            getLayoutInflater(), binding.daftarTeratas, false);
            String nama =
                    p.label == null || p.label.trim().isEmpty()
                            ? getString(R.string.m07_produk_lain)
                            : p.label.trim();
            String banyak =
                    p.qty > 0 || p.weightGram == null
                            ? getResources().getQuantityString(R.plurals.m06_item, p.qty, p.qty)
                            : CatatSisa.kg(p.weightGram);
            String ket =
                    getString(
                            R.string.m07_jumlah_nilai,
                            banyak,
                            FormatTampilan.rupiah(p.valueRupiah));
            b.nama.setText(nama);
            b.ket.setText(ket);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) b.isi.getLayoutParams();
            lp.weight = LaporanMingguan.porsi(p.valueRupiah, maks);
            b.isi.setLayoutParams(lp);
            b.getRoot().setContentDescription(nama + ", " + ket);
            binding.daftarTeratas.addView(b.getRoot());
        }
    }

    private void isiGrafik(LaporanMingguanDto l) {
        binding.grafik.removeAllViews();
        boolean cukup = l.loggedDays >= LaporanMingguan.HARI_MIN_GRAFIK;
        binding.grafik.setVisibility(cukup ? View.VISIBLE : View.GONE);
        binding.satuanGrafik.setVisibility(cukup ? View.VISIBLE : View.GONE);
        binding.ajakan.setVisibility(cukup ? View.GONE : View.VISIBLE);
        binding.ketCelah.setVisibility(View.GONE);
        if (!cukup) {
            binding.ajakanIsi.setText(
                    getResources()
                            .getQuantityString(R.plurals.m07_ajakan, l.loggedDays, l.loggedDays));
            binding.tombolCatatAjakan.setVisibility(vm.mingguIni() ? View.VISIBLE : View.GONE);
            return;
        }
        // Tujuh hari Senin..Minggu dari tanggal di respons; hari yang hilang dianggap tidak
        // dicatat.
        boolean adaCelah =
                GrafikHarian.isi(binding.grafik, vm.senin(), l.daily, BATANG_PENUH_DP, true);
        binding.ketCelah.setVisibility(adaCelah ? View.VISIBLE : View.GONE);
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
