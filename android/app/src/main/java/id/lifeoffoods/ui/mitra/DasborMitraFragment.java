package id.lifeoffoods.ui.mitra;

import android.content.res.ColorStateList;
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
import id.lifeoffoods.R;
import id.lifeoffoods.data.CatatSisa;
import id.lifeoffoods.data.DasborMitra;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.LaporanMingguan;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.RingkasanTokoDto;
import id.lifeoffoods.databinding.FragmentDasborMitraBinding;
import id.lifeoffoods.databinding.ItemTasDasborBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.util.List;

/** M05 Dashboard mitra (PRD-24): "hari ini perlu mengerjakan apa". Pemilik dan kasir. */
public class DasborMitraFragment extends Fragment {

    /** Tinggi batang tertinggi, dp (Figma M05: 124). */
    private static final int BATANG_PENUH_DP = 124;

    private FragmentDasborMitraBinding binding;
    private DasborMitraViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentDasborMitraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(DasborMitraViewModel.class);

        SisiAman.atas(binding.header);
        SisiAman.bawah(binding.nav.getRoot());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.segarkan());

        binding.pengingat.setOnClickListener(v -> buka(R.id.m06_catat));
        binding.tombolCatat.setOnClickListener(v -> buka(R.id.m06_catat));
        binding.tombolPasang.setOnClickListener(v -> buka(R.id.m09_pasang_tas));
        binding.barisPesanan.setOnClickListener(v -> buka(R.id.m11_pesanan));
        binding.tautanLaporan.setOnClickListener(v -> buka(R.id.m07_laporan));
        binding.tautanKelola.setOnClickListener(v -> buka(R.id.m10_kelola));
        NavMitra.pasang(this, binding.nav, R.id.m05_dashboard);

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.ringkasan.observe(getViewLifecycleOwner(), this::isi);
        vm.tas.observe(getViewLifecycleOwner(), this::isiTas);
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
        // Layar sebelumnya (misalnya M06) berheader terang dan memasang ikon status bar gelap.
        SisiAman.ikonGelap(
                requireActivity(), vm.status.getValue() != DasborMitraViewModel.Status.SIAP);
        // Kembali dari M06/M09/M12: angka dan pengingat ikut berubah.
        vm.segarkan();
    }

    private void tampilkanStatus() {
        DasborMitraViewModel.Status s = vm.status.getValue();
        boolean siap = s == DasborMitraViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == DasborMitraViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == DasborMitraViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == DasborMitraViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }
        // Layar memuat dan galat berlatar terang; dashboard berheader gelap.
        SisiAman.ikonGelap(requireActivity(), !siap);
    }

    private void isi(@Nullable RingkasanTokoDto r) {
        if (r == null) {
            return;
        }
        binding.namaToko.setText(r.store == null ? "" : r.store.name);
        binding.teksBuka.setText(r.isOpen ? R.string.m05_buka : R.string.m05_tutup);
        binding.titikBuka.setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                requireContext(),
                                r.isOpen ? R.color.merek_lime : R.color.ikon_redup)));

        binding.nilaiTertahan.setText(FormatTampilan.rupiah(r.rescuedValueThisWeekRupiah));
        binding.ketTertahan.setText(
                r.itemsSoldThisWeek > 0
                        ? getResources()
                                .getQuantityString(
                                        R.plurals.m05_ket_tertahan,
                                        r.itemsSoldThisWeek,
                                        r.itemsSoldThisWeek)
                        : getString(R.string.m05_ket_tertahan_kosong));

        binding.pengingat.setVisibility(r.isTodayLogged ? View.GONE : View.VISIBLE);
        binding.teksPengingat.setText(teksPengingat(r.minutesUntilClose));

        binding.barisPesanan.setVisibility(r.pendingOrdersToday > 0 ? View.VISIBLE : View.GONE);
        binding.teksPesanan.setText(
                getResources()
                        .getQuantityString(
                                R.plurals.m05_pesanan_menunggu,
                                r.pendingOrdersToday,
                                r.pendingOrdersToday));

        String sisa =
                r.avgDailyWasteGram == null
                        ? getString(R.string.m05_sisa_kosong)
                        : CatatSisa.kg(r.avgDailyWasteGram);
        binding.nilaiSisa.setText(sisa);
        binding.ubinSisa.setContentDescription(getString(R.string.m05_ubin_sisa, sisa));
        String tas =
                getResources()
                        .getQuantityString(
                                R.plurals.m05_tas, r.unsoldBagsLast7Days, r.unsoldBagsLast7Days);
        binding.nilaiTas.setText(tas);
        binding.ubinTas.setContentDescription(getString(R.string.m05_ubin_tas, tas));

        isiGrafik(r);
    }

    private String teksPengingat(@Nullable Integer menit) {
        DasborMitra.SisaWaktu w = DasborMitra.sisaWaktu(menit);
        if (w == null) {
            return getString(R.string.m05_belum_catat);
        }
        return getResources()
                .getQuantityString(
                        w.jam ? R.plurals.m05_tutup_jam : R.plurals.m05_tutup_menit,
                        w.angka,
                        w.angka);
    }

    private void isiGrafik(RingkasanTokoDto r) {
        boolean ada = DasborMitra.adaCatatan(r.daily);
        binding.grafik.setVisibility(ada ? View.VISIBLE : View.GONE);
        if (ada) {
            LocalDate senin = LaporanMingguan.senin(LocalDate.now(PesananMasukViewModel.WIB));
            GrafikHarian.isi(binding.grafik, senin, r.daily, BATANG_PENUH_DP, false);
        }
        binding.ketGrafik.setText(
                !ada
                        ? getString(R.string.m05_grafik_kosong)
                        : r.peakDay == null ? "" : getString(R.string.m05_puncak, r.peakDay));
        binding.ketGrafik.setVisibility(
                binding.ketGrafik.getText().length() > 0 ? View.VISIBLE : View.GONE);
    }

    private void isiTas(@Nullable List<JualanMitraDto> tas) {
        binding.kartuTas.setVisibility(tas == null ? View.GONE : View.VISIBLE);
        if (tas == null) {
            return;
        }
        binding.daftarTas.removeAllViews();
        binding.tasKosong.setVisibility(tas.isEmpty() ? View.VISIBLE : View.GONE);
        for (JualanMitraDto j : tas) {
            ItemTasDasborBinding b =
                    ItemTasDasborBinding.inflate(getLayoutInflater(), binding.daftarTas, false);
            b.judul.setText(j.title);
            String ket;
            if (JualanMitraDto.HABIS.equals(j.status)) {
                ket = getString(R.string.m05_tas_habis, j.qtyTotal);
            } else if (JualanMitraDto.LEWAT.equals(j.status)) {
                ket = getString(R.string.m05_tas_lewat, j.qtyRemaining, j.qtyTotal);
            } else {
                ket =
                        getString(
                                R.string.m05_tas_ket,
                                getString(R.string.m10_sisa, j.qtyRemaining, j.qtyTotal),
                                FormatTampilan.jam(j.pickupStart));
            }
            b.ket.setText(ket);
            android.widget.TextView pil =
                    KelolaJualanFragment.pilStatus(requireContext(), j.status);
            b.status.addView(pil);
            b.getRoot().setContentDescription(j.title + ", " + ket + ", " + pil.getText());
            binding.daftarTas.addView(b.getRoot());
        }
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
