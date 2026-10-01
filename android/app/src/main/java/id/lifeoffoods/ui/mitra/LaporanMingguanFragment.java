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
import id.lifeoffoods.databinding.ItemBatangHarianBinding;
import id.lifeoffoods.databinding.ItemProdukTeratasBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** M07 Laporan mingguan (PRD-13). Hanya pemilik; kasir melihat keterangan, bukan galat. */
public class LaporanMingguanFragment extends Fragment {

    private static final Locale ID = new Locale("id", "ID");
    private static final DateTimeFormatter HARI_PANJANG = DateTimeFormatter.ofPattern("EEEE", ID);

    /** Tinggi penuh batang dan tinggi minimum batang bernilai, dp (Figma: 100). */
    private static final int BATANG_PENUH_DP = 100;

    private static final int BATANG_MIN_DP = 4;
    private static final int GARIS_NOL_DP = 2;
    private static final int CELAH_DP = 24;

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
        LocalDate senin = vm.senin();
        List<Long> nilai = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            nilai.add(nilaiHari(l.daily, senin.plusDays(i)));
        }
        long maks = LaporanMingguan.maks(nilai);
        float dp = getResources().getDisplayMetrics().density;
        boolean adaCelah = false;
        for (int i = 0; i < 7; i++) {
            LocalDate hari = senin.plusDays(i);
            Long n = nilai.get(i);
            boolean teratas = n != null && maks > 0 && n == maks;
            ItemBatangHarianBinding b =
                    ItemBatangHarianBinding.inflate(getLayoutInflater(), binding.grafik, false);
            b.hari.setText(hari.getDayOfWeek().getDisplayName(TextStyle.SHORT, ID));
            String panjang = kapital(hari.format(HARI_PANJANG));
            ViewGroup.LayoutParams lp = b.batang.getLayoutParams();
            if (n == null) {
                adaCelah = true;
                b.angka.setText("–");
                b.batang.setBackgroundResource(R.drawable.bg_batang_kosong);
                lp.height = Math.round(CELAH_DP * dp);
                b.getRoot().setContentDescription(getString(R.string.m07_hari_kosong, panjang));
            } else {
                int t =
                        LaporanMingguan.tinggi(
                                n,
                                maks,
                                Math.round(BATANG_PENUH_DP * dp),
                                Math.round(BATANG_MIN_DP * dp));
                lp.height = t > 0 ? t : Math.round(GARIS_NOL_DP * dp);
                b.angka.setText(LaporanMingguan.ribu(n));
                b.batang.setBackgroundResource(
                        teratas
                                ? R.drawable.bg_batang_harian_aktif
                                : R.drawable.bg_batang_harian_pasif);
                if (teratas) {
                    b.angka.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.teks_merek));
                }
                b.getRoot()
                        .setContentDescription(
                                getString(
                                        teratas
                                                ? R.string.m07_hari_teratas
                                                : R.string.m07_hari_nilai,
                                        panjang,
                                        FormatTampilan.rupiah(n)));
            }
            b.batang.setLayoutParams(lp);
            binding.grafik.addView(b.getRoot());
        }
        binding.ketCelah.setVisibility(adaCelah ? View.VISIBLE : View.GONE);
    }

    @Nullable
    private static Long nilaiHari(@Nullable List<LaporanMingguanDto.Harian> daily, LocalDate hari) {
        if (daily == null) {
            return null;
        }
        for (LaporanMingguanDto.Harian h : daily) {
            try {
                if (h.date != null && LocalDate.parse(h.date).equals(hari)) {
                    return h.wastedValueRupiah;
                }
            } catch (DateTimeParseException e) {
                // Tanggal rusak dilewati; hari itu tampil sebagai tidak dicatat.
            }
        }
        return null;
    }

    private static String kapital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
