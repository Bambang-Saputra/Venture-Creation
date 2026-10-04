package id.lifeoffoods.ui.mitra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.RiwayatMitra;
import id.lifeoffoods.data.api.model.SaldoTokoDto;
import id.lifeoffoods.databinding.FragmentSaldoTokoBinding;
import id.lifeoffoods.databinding.ItemTransaksiSaldoBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.util.List;

/** M13 Saldo dan pencairan (PRD-16). Dari baris "Saldo dan pencairan" di M14. Hanya pemilik. */
public class SaldoTokoFragment extends Fragment {

    private FragmentSaldoTokoBinding binding;
    private SaldoTokoViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSaldoTokoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(SaldoTokoViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.isi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.m13_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolMuatLagi.setOnClickListener(v -> vm.muatLagi());

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.saldo.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.transaksi.observe(getViewLifecycleOwner(), t -> tampilkan());
        vm.adaBerikutnya.observe(getViewLifecycleOwner(), a -> tampilkan());
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
        SaldoTokoViewModel.Status s = vm.status.getValue();
        boolean siap = s == SaldoTokoViewModel.Status.SIAP;
        boolean bukanPemilik = s == SaldoTokoViewModel.Status.BUKAN_PEMILIK;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == SaldoTokoViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == SaldoTokoViewModel.Status.GAGAL || bukanPemilik ? View.VISIBLE : View.GONE);
        binding.keadaan.tombolCobaLagi.setVisibility(bukanPemilik ? View.GONE : View.VISIBLE);
        if (bukanPemilik) {
            binding.keadaan.gagalIsi.setText(R.string.m13_bukan_pemilik);
        } else if (s == SaldoTokoViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }

        SaldoTokoDto d = vm.saldo.getValue();
        if (!siap || d == null) {
            return;
        }
        binding.saldo.setText(FormatTampilan.rupiah(d.availableRupiah));
        binding.dari.setText(
                getResources()
                        .getQuantityString(
                                R.plurals.m13_dari_terjual,
                                d.itemsSoldThisWeek,
                                d.itemsSoldThisWeek));
        binding.tertunda.setVisibility(d.pendingRupiah > 0 ? View.VISIBLE : View.GONE);
        binding.tertunda.setText(
                getString(R.string.m13_tertunda, FormatTampilan.rupiah(d.pendingRupiah)));
        boolean bisa = d.withdrawal != null && d.withdrawal.enabled;
        binding.tombolCairkan.setEnabled(bisa);
        String alasan =
                d.withdrawal != null && d.withdrawal.reason != null
                        ? d.withdrawal.reason
                        : getString(R.string.m13_pencairan_nonaktif);
        binding.alasan.setVisibility(bisa ? View.GONE : View.VISIBLE);
        binding.alasan.setText(alasan);

        isiRiwayat(vm.transaksi.getValue());
        binding.tombolMuatLagi.setVisibility(
                Boolean.TRUE.equals(vm.adaBerikutnya.getValue()) ? View.VISIBLE : View.GONE);
    }

    private void isiRiwayat(@Nullable List<SaldoTokoDto.Transaksi> daftar) {
        binding.daftar.removeAllViews();
        boolean kosong = daftar == null || daftar.isEmpty();
        binding.kosong.setVisibility(kosong ? View.VISIBLE : View.GONE);
        if (kosong) {
            return;
        }
        LocalDate hariIni = LocalDate.now(PesananMasukViewModel.WIB);
        for (SaldoTokoDto.Transaksi t : daftar) {
            ItemTransaksiSaldoBinding b =
                    ItemTransaksiSaldoBinding.inflate(getLayoutInflater(), binding.daftar, false);
            b.ikon.setImageResource(ikon(t.type));
            b.judul.setText(judul(t));
            b.waktu.setText(RiwayatMitra.waktu(t.createdAt, hariIni));
            boolean masuk = t.amountRupiah > 0;
            String nominal =
                    t.amountRupiah == 0
                            ? FormatTampilan.rupiah(0)
                            : (masuk ? "+" : "−") + FormatTampilan.rupiah(Math.abs(t.amountRupiah));
            b.nominal.setText(nominal);
            b.nominal.setTextColor(
                    ContextCompat.getColor(
                            requireContext(), masuk ? R.color.teks_merek : R.color.teks_utama));
            b.getRoot()
                    .setContentDescription(
                            b.judul.getText() + ", " + b.waktu.getText() + ", " + nominal);
            binding.daftar.addView(b.getRoot());
        }
    }

    private String judul(SaldoTokoDto.Transaksi t) {
        if (SaldoTokoDto.Transaksi.PENJUALAN.equals(t.type) && t.title != null) {
            return t.pickupCode == null
                    ? t.title
                    : getString(R.string.m13_judul_jual, t.title, t.pickupCode);
        }
        if (t.description != null && !t.description.isEmpty()) {
            return t.description;
        }
        if (SaldoTokoDto.Transaksi.PENCAIRAN.equals(t.type)) {
            return getString(R.string.m13_pencairan);
        }
        return "service_fee".equals(t.type)
                ? getString(R.string.m13_biaya)
                : getString(R.string.m13_penyesuaian);
    }

    @DrawableRes
    private static int ikon(@Nullable String tipe) {
        if (SaldoTokoDto.Transaksi.PENJUALAN.equals(tipe)) {
            return R.drawable.ic_tas;
        }
        if (SaldoTokoDto.Transaksi.PENCAIRAN.equals(tipe)) {
            return R.drawable.ic_bank;
        }
        return R.drawable.ic_dompet;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
