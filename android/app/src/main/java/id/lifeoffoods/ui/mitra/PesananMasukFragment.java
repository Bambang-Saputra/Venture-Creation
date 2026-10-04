package id.lifeoffoods.ui.mitra;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.PesananMitra;
import id.lifeoffoods.data.RiwayatMitra;
import id.lifeoffoods.data.api.model.HalamanPesananMitra;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import id.lifeoffoods.databinding.FragmentPesananMasukBinding;
import id.lifeoffoods.databinding.IncludeUbinRingkasBinding;
import id.lifeoffoods.databinding.ItemPesananMasukBinding;
import id.lifeoffoods.databinding.ItemRiwayatMitraBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * M11 Pesanan masuk (PRD-10). Kartu dan tombol "Cocokkan kode" membuka M12. Catatan dan alergi
 * pembeli tampil langsung di kartu, alergi berat paling mencolok.
 */
public class PesananMasukFragment extends Fragment {

    private FragmentPesananMasukBinding binding;
    private PesananMasukViewModel vm;
    private RiwayatPesananMitraViewModel riwayatVm;
    private BaseListAdapter<PesananMitraDto, ItemPesananMasukBinding> adapter;
    private BaseListAdapter<PesananMitraDto, ItemRiwayatMitraBinding> adapterRiwayat;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentPesananMasukBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(PesananMasukViewModel.class);

        SisiAman.atas(binding.header);
        SisiAman.bawah(binding.nav.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);

        ikon16(binding.tombolCocokkan, R.drawable.ic_pindai);
        binding.tombolCocokkan.setOnClickListener(v -> keCocokkan());
        binding.tabHariIni.setOnClickListener(v -> vm.pilihTab(PesananMasukViewModel.Tab.HARI_INI));
        binding.tabRiwayat.setOnClickListener(v -> vm.pilihTab(PesananMasukViewModel.Tab.RIWAYAT));
        binding.tombolCobaLagi.setOnClickListener(
                v -> {
                    if (vm.tab.getValue() == PesananMasukViewModel.Tab.RIWAYAT) {
                        riwayatVm.muatUlang();
                    } else {
                        vm.muatUlang();
                    }
                });
        binding.tombolKelola.setOnClickListener(v -> NavMitra.bukaKelola(this));

        ubin(binding.ubinMenunggu, R.string.m11_menunggu, R.color.tanda_proses_teks);
        ubin(binding.ubinDiambil, R.string.m11_diambil, R.color.teks_merek);
        ubin(binding.ubinTidak, R.string.m11_tidak_diambil, R.color.tanda_bahaya_teks);

        adapter =
                new BaseListAdapter<>(ItemPesananMasukBinding::inflate, this::isiKartu, p -> p.id);
        adapterRiwayat =
                new BaseListAdapter<>(
                        ItemRiwayatMitraBinding::inflate, this::isiBarisRiwayat, p -> p.id);
        binding.daftar.setAdapter(adapter);

        // M21: saringan rentang dan halaman berikutnya.
        riwayatVm = new ViewModelProvider(this).get(RiwayatPesananMitraViewModel.class);
        binding.saring7.setOnClickListener(
                v -> riwayatVm.pilih(RiwayatPesananMitraViewModel.Saringan.TUJUH_HARI));
        binding.saring30.setOnClickListener(
                v -> riwayatVm.pilih(RiwayatPesananMitraViewModel.Saringan.TIGA_PULUH_HARI));
        binding.saringSemua.setOnClickListener(
                v -> riwayatVm.pilih(RiwayatPesananMitraViewModel.Saringan.SEMUA));
        binding.tombolMuatLagi.setOnClickListener(v -> riwayatVm.muatLagi());
        riwayatVm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        riwayatVm.daftar.observe(getViewLifecycleOwner(), d -> tampilkan());
        riwayatVm.ringkasan.observe(getViewLifecycleOwner(), r -> tampilkan());
        riwayatVm.adaBerikutnya.observe(getViewLifecycleOwner(), a -> tampilkan());
        riwayatVm.memuatLagi.observe(
                getViewLifecycleOwner(),
                m -> binding.tombolMuatLagi.setEnabled(!Boolean.TRUE.equals(m)));
        riwayatVm.galatLagi.observe(
                getViewLifecycleOwner(),
                p -> {
                    String teks = p.ambil();
                    if (teks != null) {
                        Snackbar.make(binding.getRoot(), teks, Snackbar.LENGTH_LONG)
                                .setAnchorView(binding.nav.getRoot())
                                .show();
                    }
                });
        riwayatVm.sesiBerakhir.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        ((MainActivity) requireActivity()).sesiBerakhir();
                    }
                });

        NavMitra.pasang(this, binding.nav, R.id.m11_pesanan);

        vm.tab.observe(getViewLifecycleOwner(), t -> tampilkan());
        vm.menunggu.observe(getViewLifecycleOwner(), d -> tampilkan());
        vm.riwayat.observe(getViewLifecycleOwner(), d -> tampilkan());
        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.jualanAktif.observe(getViewLifecycleOwner(), n -> tampilkan());
        vm.hitungan.observe(
                getViewLifecycleOwner(),
                h -> {
                    binding.ubinMenunggu.angka.setText(String.valueOf(h.menunggu));
                    binding.ubinDiambil.angka.setText(String.valueOf(h.diambil));
                    binding.ubinTidak.angka.setText(String.valueOf(h.tidakDiambil));
                });
        vm.luringSejak.observe(
                getViewLifecycleOwner(),
                w -> {
                    binding.luring.setVisibility(w == null ? View.GONE : View.VISIBLE);
                    if (w != null) {
                        binding.luring.setText(
                                getString(
                                        R.string.m11_luring,
                                        DateFormat.format("HH.mm", new Date(w)).toString()));
                    }
                });
        vm.galatAwal.observe(getViewLifecycleOwner(), g -> tampilkan());
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
        vm.terlihat();
    }

    @Override
    public void onStop() {
        super.onStop();
        vm.tersembunyi();
    }

    private void tampilkan() {
        if (binding == null || riwayatVm == null) {
            return;
        }
        boolean hariIni = vm.tab.getValue() != PesananMasukViewModel.Tab.RIWAYAT;
        gayaTab(binding.tabHariIni, hariIni);
        gayaTab(binding.tabRiwayat, !hariIni);
        if (!hariIni) {
            tampilkanRiwayat();
            return;
        }
        binding.kepalaRiwayat.setVisibility(View.GONE);
        binding.ringkasan.setVisibility(View.VISIBLE);
        binding.tombolMuatLagi.setVisibility(View.GONE);
        if (binding.daftar.getAdapter() != adapter) {
            binding.daftar.setAdapter(adapter);
        }

        PesananMasukViewModel.Status s = vm.status.getValue();
        boolean siap = s == PesananMasukViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.progres.setVisibility(
                s == PesananMasukViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.gagal.setVisibility(
                s == PesananMasukViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == PesananMasukViewModel.Status.GAGAL && vm.galatAwal.getValue() != null) {
            binding.gagalIsi.setText(vm.galatAwal.getValue());
        }

        List<PesananMitraDto> d = vm.menunggu.getValue();
        adapter.submitList(d);
        boolean kosong = d == null || d.isEmpty();
        binding.kosong.setVisibility(siap && kosong ? View.VISIBLE : View.GONE);
        binding.kosongTeks.setText(R.string.m11_kosong_hari_ini);
        Integer aktif = vm.jualanAktif.getValue();
        boolean tampilJualan = siap && kosong;
        binding.kosongJualan.setVisibility(
                tampilJualan && aktif != null ? View.VISIBLE : View.GONE);
        if (aktif != null) {
            binding.kosongJualan.setText(getString(R.string.m11_jualan_aktif, aktif));
        }
        binding.tombolKelola.setVisibility(tampilJualan ? View.VISIBLE : View.GONE);
    }

    /** M21: tab Riwayat memakai data sendiri (rentang hari dan halaman), bukan riwayat hari ini. */
    private void tampilkanRiwayat() {
        riwayatVm.pastikanTermuat();
        binding.kepalaRiwayat.setVisibility(View.VISIBLE);
        binding.ringkasan.setVisibility(View.GONE);
        binding.kosongJualan.setVisibility(View.GONE);
        binding.tombolKelola.setVisibility(View.GONE);
        if (binding.daftar.getAdapter() != adapterRiwayat) {
            binding.daftar.setAdapter(adapterRiwayat);
        }

        RiwayatPesananMitraViewModel.Saringan saring = riwayatVm.saringan.getValue();
        binding.saring7.setChecked(saring == RiwayatPesananMitraViewModel.Saringan.TUJUH_HARI);
        binding.saring30.setChecked(
                saring == RiwayatPesananMitraViewModel.Saringan.TIGA_PULUH_HARI);
        binding.saringSemua.setChecked(saring == RiwayatPesananMitraViewModel.Saringan.SEMUA);

        RiwayatPesananMitraViewModel.Status s = riwayatVm.status.getValue();
        boolean siap = s == RiwayatPesananMitraViewModel.Status.SIAP;
        boolean gagal = s == RiwayatPesananMitraViewModel.Status.GAGAL;
        // Kepala saringan tetap terlihat saat memuat, supaya pilihan rentang bisa diganti lagi.
        binding.gulir.setVisibility(View.VISIBLE);
        binding.keadaan.setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.progres.setVisibility(siap || gagal ? View.GONE : View.VISIBLE);
        binding.gagal.setVisibility(gagal ? View.VISIBLE : View.GONE);
        if (gagal && riwayatVm.galat.getValue() != null) {
            binding.gagalIsi.setText(riwayatVm.galat.getValue());
        }

        List<PesananMitraDto> d = siap ? riwayatVm.daftar.getValue() : null;
        adapterRiwayat.submitList(d);
        boolean kosong = d == null || d.isEmpty();
        binding.kosong.setVisibility(siap && kosong ? View.VISIBLE : View.GONE);
        binding.kosongTeks.setText(R.string.m21_kosong);
        binding.tombolMuatLagi.setVisibility(
                siap && Boolean.TRUE.equals(riwayatVm.adaBerikutnya.getValue())
                        ? View.VISIBLE
                        : View.GONE);

        HalamanPesananMitra.Ringkasan r = riwayatVm.ringkasan.getValue();
        binding.ringkasanRiwayat.setVisibility(siap && r != null ? View.VISIBLE : View.GONE);
        if (r != null) {
            binding.teksRingkasanRiwayat.setText(
                    getString(
                            R.string.m21_ringkasan,
                            getString(rentang(saring)),
                            r.completed,
                            r.noShow));
        }
    }

    @StringRes
    private static int rentang(@Nullable RiwayatPesananMitraViewModel.Saringan s) {
        if (s == RiwayatPesananMitraViewModel.Saringan.TIGA_PULUH_HARI) {
            return R.string.m21_rentang_30;
        }
        if (s == RiwayatPesananMitraViewModel.Saringan.SEMUA) {
            return R.string.m21_rentang_semua;
        }
        return R.string.m21_rentang_7;
    }

    private void isiBarisRiwayat(ItemRiwayatMitraBinding b, PesananMitraDto p) {
        List<PesananMitraDto> semua = adapterRiwayat.getCurrentList();
        int posisi = semua.indexOf(p);
        String sebelumnya = posisi > 0 ? semua.get(posisi - 1).pickupStart : null;
        boolean awal = posisi <= 0 || RiwayatMitra.awalKelompok(sebelumnya, p.pickupStart);
        LocalDate tanggal = RiwayatMitra.tanggal(p.pickupStart);
        b.judulHari.setVisibility(awal && tanggal != null ? View.VISIBLE : View.GONE);
        if (tanggal != null) {
            b.judulHari.setText(
                    RiwayatMitra.judulHari(tanggal, LocalDate.now(PesananMasukViewModel.WIB)));
        }
        isiKartu(b.kartu, p);
        // Figma M21: kode pickup menggantikan cara bayar, jam ambil disembunyikan.
        b.kartu.jam.setVisibility(View.GONE);
        if (p.pickupCode != null) {
            b.kartu.bayar.setText(getString(R.string.m21_kode, p.pickupCode));
        }
    }

    private void isiKartu(ItemPesananMasukBinding b, PesananMitraDto p) {
        b.pembeli.setText(p.buyerName);
        b.item.setText(PesananMitra.ringkasItem(p.items));
        b.total.setText(FormatTampilan.rupiah(p.totalRupiah));
        b.bayar.setText(TampilanPesananMitra.caraBayar(requireContext(), p.paymentMethod));
        b.jam.setText(
                getString(
                        R.string.m11_ambil_jam,
                        FormatTampilan.rentangJam(p.pickupStart, p.pickupEnd)));
        isiStatus(b.status, p);
        TampilanPesananMitra.isiCatatan(requireContext(), b.catatan, p);

        boolean menunggu = PesananDto.MENUNGGU_DIAMBIL.equals(p.status);
        // setOnClickListener selalu membuat view clickable, jadi clickable diatur sesudahnya.
        b.getRoot().setOnClickListener(menunggu ? v -> keCocokkan() : null);
        b.getRoot().setClickable(menunggu);
        b.getRoot().setFocusable(menunggu);
        b.getRoot()
                .setContentDescription(
                        menunggu
                                ? getString(
                                        R.string.m11_buka_kartu,
                                        p.buyerName,
                                        PesananMitra.ringkasItem(p.items))
                                : null);
    }

    private void isiStatus(TextView tv, PesananMitraDto p) {
        if (PesananDto.SELESAI.equals(p.status)) {
            String jam = FormatTampilan.jam(p.completedAt);
            pil(
                    tv,
                    jam.isEmpty()
                            ? getString(R.string.m11_diambil)
                            : getString(R.string.m11_diambil_jam, jam),
                    R.drawable.bg_pil_merek,
                    R.color.tanda_hemat_teks);
        } else if (PesananDto.TIDAK_DIAMBIL.equals(p.status)) {
            pil(
                    tv,
                    getString(R.string.m11_tidak_diambil),
                    R.drawable.bg_pil_bahaya,
                    R.color.tanda_bahaya_teks);
        } else if (PesananDto.DIBATALKAN.equals(p.status)) {
            pil(tv, getString(R.string.m11_dibatalkan), R.drawable.bg_pil_isian, R.color.teks_kuat);
        } else {
            pil(
                    tv,
                    getString(R.string.m11_menunggu),
                    R.drawable.bg_pil_proses,
                    R.color.tanda_proses_teks);
        }
    }

    private void pil(TextView tv, String teks, @DrawableRes int latar, @ColorRes int warna) {
        tv.setText(teks);
        tv.setBackgroundResource(latar);
        tv.setTextColor(ContextCompat.getColor(requireContext(), warna));
    }

    private void ubin(IncludeUbinRingkasBinding u, @StringRes int label, @ColorRes int warna) {
        u.label.setText(label);
        u.angka.setText("0");
        u.angka.setTextColor(ContextCompat.getColor(requireContext(), warna));
    }

    private void gayaTab(TextView tv, boolean terpilih) {
        tv.setSelected(terpilih);
        tv.setBackgroundResource(terpilih ? R.drawable.bg_tab_aktif : 0);
        tv.setTextColor(
                ContextCompat.getColor(
                        requireContext(), terpilih ? R.color.teks_merek : R.color.teks_sekunder));
    }

    /** Ikon 16dp putih di depan teks tombol pil (Figma "Ikon/pindai" 16). */
    private void ikon16(TextView tv, @DrawableRes int ikon) {
        @Nullable Drawable d = AppCompatResources.getDrawable(requireContext(), ikon);
        if (d == null) {
            return;
        }
        d = DrawableCompat.wrap(d.mutate());
        DrawableCompat.setTint(d, ContextCompat.getColor(requireContext(), R.color.teks_di_gelap));
        int ukuran = Math.round(16 * getResources().getDisplayMetrics().density);
        d.setBounds(0, 0, ukuran, ukuran);
        tv.setCompoundDrawablesRelative(d, null, null, null);
    }

    private void keCocokkan() {
        buka(R.id.m12_cocokkan);
    }

    private void buka(int tujuan) {
        NavHostFragment.findNavController(this).navigate(tujuan);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.daftar.setAdapter(null);
        binding = null;
    }
}
