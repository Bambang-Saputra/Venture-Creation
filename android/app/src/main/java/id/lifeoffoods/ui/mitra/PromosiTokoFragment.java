package id.lifeoffoods.ui.mitra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.PromosiToko;
import id.lifeoffoods.data.api.model.PromosiDto;
import id.lifeoffoods.databinding.FragmentPromosiTokoBinding;
import id.lifeoffoods.databinding.ItemPaketPromosiBinding;
import id.lifeoffoods.databinding.ItemTransaksiSaldoBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.jualan.Stepper;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;

/**
 * M22 Promosikan toko (#3). Mitra membeli iklan per hari tayang: prioritas pencarian atau banner
 * Beranda. Selama uji coba belum ditagih; layar tetap menampilkan total supaya mitra tahu harganya.
 */
public class PromosiTokoFragment extends Fragment {

    private FragmentPromosiTokoBinding binding;
    private PromosiTokoViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPromosiTokoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PromosiTokoViewModel.class);
        vm.muat();

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.isi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.m22_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigateUp());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());

        binding.paketPrioritas.ikon.setImageResource(R.drawable.ic_cari);
        binding.paketBanner.ikon.setImageResource(R.drawable.ic_gambar);
        binding.paketPrioritas
                .getRoot()
                .setOnClickListener(v -> vm.pilihPaket(PromosiDto.PRIORITAS));
        binding.paketBanner.getRoot().setOnClickListener(v -> vm.pilihPaket(PromosiDto.BANNER));
        binding.tombolAktifkan.setOnClickListener(
                v ->
                        vm.aktifkan(
                                binding.kalimat.getText() == null
                                        ? null
                                        : binding.kalimat.getText().toString()));

        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanStatus());
        vm.halaman.observe(getViewLifecycleOwner(), h -> tampilkan());
        vm.paket.observe(getViewLifecycleOwner(), p -> tampilkan());
        vm.hari.observe(getViewLifecycleOwner(), h -> tampilkan());
        vm.menyimpan.observe(
                getViewLifecycleOwner(),
                m -> {
                    binding.tombolAktifkan.setEnabled(!m);
                    binding.tombolAktifkan.setText(
                            m ? R.string.m22_menyimpan : R.string.m22_aktifkan);
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

    private void tampilkanStatus() {
        PromosiTokoViewModel.Status s = vm.status.getValue();
        boolean siap = s == PromosiTokoViewModel.Status.SIAP;
        boolean bukanPemilik = s == PromosiTokoViewModel.Status.BUKAN_PEMILIK;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == PromosiTokoViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == PromosiTokoViewModel.Status.GAGAL || bukanPemilik ? View.VISIBLE : View.GONE);
        binding.keadaan.tombolCobaLagi.setVisibility(bukanPemilik ? View.GONE : View.VISIBLE);
        if (bukanPemilik) {
            binding.keadaan.gagalIsi.setText(R.string.m22_bukan_pemilik);
        } else if (s == PromosiTokoViewModel.Status.GAGAL && vm.galat.getValue() != null) {
            binding.keadaan.gagalIsi.setText(vm.galat.getValue());
        }
    }

    private void tampilkan() {
        PromosiDto.Halaman h = vm.halaman.getValue();
        if (binding == null || h == null) {
            return;
        }
        if (h.billing != null && h.billing.reason != null && !h.billing.enabled) {
            binding.infoTagihan.setText(h.billing.reason);
        }

        String terpilih = vm.paket.getValue();
        isiPaket(binding.paketPrioritas, cari(h.packages, PromosiDto.PRIORITAS), terpilih);
        isiPaket(binding.paketBanner, cari(h.packages, PromosiDto.BANNER), terpilih);
        binding.isianKalimat.setVisibility(
                PromosiDto.BANNER.equals(terpilih) ? View.VISIBLE : View.GONE);

        int hari = vm.hari.getValue() == null ? PromosiToko.HARI_AWAL : vm.hari.getValue();
        Stepper.isi(
                binding.stepper,
                hari,
                hari > PromosiToko.HARI_MIN,
                hari < PromosiToko.HARI_MAKS,
                () -> vm.ubahHari(-1),
                () -> vm.ubahHari(1));
        binding.stepper.jumlah.setContentDescription(
                getResources().getQuantityString(R.plurals.m22_hari, hari, hari));
        binding.rentang.setText(
                getString(R.string.m22_tayang, PromosiToko.rentang(vm.mulai(), hari)));

        long tarif = vm.tarif();
        binding.total.setText(FormatTampilan.rupiah(PromosiToko.total(tarif, hari)));
        binding.rincian.setText(
                getString(
                        R.string.m22_rincian,
                        FormatTampilan.rupiah(tarif),
                        getResources().getQuantityString(R.plurals.m22_hari, hari, hari)));

        isiRiwayat(h.data);
    }

    private void isiPaket(
            ItemPaketPromosiBinding b, @Nullable PromosiDto.Paket p, @Nullable String terpilih) {
        b.getRoot().setVisibility(p == null ? View.GONE : View.VISIBLE);
        if (p == null) {
            return;
        }
        boolean pilih = p.code.equals(terpilih);
        b.getRoot().setSelected(pilih);
        b.nama.setText(p.name);
        b.ket.setText(p.description);
        b.tarif.setText(
                getString(R.string.m22_per_hari, FormatTampilan.rupiah(p.pricePerDayRupiah)));
        b.getRoot()
                .setContentDescription(
                        p.name
                                + ", "
                                + b.tarif.getText()
                                + ". "
                                + getString(pilih ? R.string.m22_terpilih : R.string.m22_pilih));
    }

    private void isiRiwayat(@Nullable List<PromosiDto> daftar) {
        binding.daftar.removeAllViews();
        boolean kosong = daftar == null || daftar.isEmpty();
        binding.kosong.setVisibility(kosong ? View.VISIBLE : View.GONE);
        if (kosong) {
            return;
        }
        for (PromosiDto p : daftar) {
            ItemTransaksiSaldoBinding b =
                    ItemTransaksiSaldoBinding.inflate(getLayoutInflater(), binding.daftar, false);
            b.ikon.setImageResource(ikon(p.paket));
            b.judul.setText(
                    getString(
                            R.string.m22_baris,
                            getString(
                                    PromosiDto.BANNER.equals(p.paket)
                                            ? R.string.m22_paket_banner
                                            : R.string.m22_paket_prioritas),
                            getString(status(p.status))));
            String rentang = PromosiToko.rentang(p.startsOn, p.endsOn);
            b.waktu.setText(rentang == null ? "" : rentang);
            b.nominal.setText(FormatTampilan.rupiah(p.totalRupiah));
            b.getRoot()
                    .setContentDescription(
                            b.judul.getText()
                                    + ", "
                                    + b.waktu.getText()
                                    + ", "
                                    + b.nominal.getText());
            binding.daftar.addView(b.getRoot());
        }
    }

    @Nullable
    private static PromosiDto.Paket cari(@Nullable List<PromosiDto.Paket> daftar, String kode) {
        if (daftar == null) {
            return null;
        }
        for (PromosiDto.Paket p : daftar) {
            if (kode.equals(p.code)) {
                return p;
            }
        }
        return null;
    }

    @DrawableRes
    private static int ikon(@Nullable String paket) {
        return PromosiDto.BANNER.equals(paket) ? R.drawable.ic_gambar : R.drawable.ic_cari;
    }

    private static int status(@Nullable String s) {
        if ("active".equals(s)) {
            return R.string.m22_status_tayang;
        }
        if ("scheduled".equals(s)) {
            return R.string.m22_status_terjadwal;
        }
        return R.string.m22_status_selesai;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
