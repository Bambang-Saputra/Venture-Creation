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
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.PesananMitra;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.PesananMitraDto;
import id.lifeoffoods.databinding.FragmentPesananMasukBinding;
import id.lifeoffoods.databinding.IncludeUbinRingkasBinding;
import id.lifeoffoods.databinding.ItemPesananMasukBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.Date;
import java.util.List;

/**
 * M11 Pesanan masuk (PRD-10). Kartu dan tombol "Cocokkan kode" membuka M12. Catatan dan alergi
 * pembeli tampil langsung di kartu, alergi berat paling mencolok.
 */
public class PesananMasukFragment extends Fragment {

    private FragmentPesananMasukBinding binding;
    private PesananMasukViewModel vm;
    private BaseListAdapter<PesananMitraDto, ItemPesananMasukBinding> adapter;

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
        binding.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());

        ubin(binding.ubinMenunggu, R.string.m11_menunggu, R.color.tanda_proses_teks);
        ubin(binding.ubinDiambil, R.string.m11_diambil, R.color.teks_merek);
        ubin(binding.ubinTidak, R.string.m11_tidak_diambil, R.color.tanda_bahaya_teks);

        adapter =
                new BaseListAdapter<>(ItemPesananMasukBinding::inflate, this::isiKartu, p -> p.id);
        binding.daftar.setAdapter(adapter);

        // Pesanan (M11) adalah layar ini. Toko membuka Kelola jualan (M10) selama M14 Profil toko
        // belum ada.
        binding.nav.tabPesanan.setSelected(true);
        // Selama M05 belum dibuat, Beranda membuka M07 Laporan mingguan.
        binding.nav.tabBeranda.setOnClickListener(v -> buka(R.id.m07_laporan));
        binding.nav.tabCatat.setOnClickListener(v -> buka(R.id.m06_catat));
        binding.nav.tabToko.setOnClickListener(v -> buka(R.id.m10_kelola));

        vm.tab.observe(getViewLifecycleOwner(), t -> tampilkan());
        vm.menunggu.observe(getViewLifecycleOwner(), d -> tampilkan());
        vm.riwayat.observe(getViewLifecycleOwner(), d -> tampilkan());
        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
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
        vm.galatAwal.observe(getViewLifecycleOwner(), binding.gagalIsi::setText);
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
        PesananMasukViewModel.Status s = vm.status.getValue();
        boolean siap = s == PesananMasukViewModel.Status.SIAP;
        binding.gulir.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.progres.setVisibility(
                s == PesananMasukViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.gagal.setVisibility(
                s == PesananMasukViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);

        boolean hariIni = vm.tab.getValue() != PesananMasukViewModel.Tab.RIWAYAT;
        gayaTab(binding.tabHariIni, hariIni);
        gayaTab(binding.tabRiwayat, !hariIni);

        List<PesananMitraDto> d = hariIni ? vm.menunggu.getValue() : vm.riwayat.getValue();
        adapter.submitList(d);
        boolean kosong = d == null || d.isEmpty();
        binding.kosong.setVisibility(siap && kosong ? View.VISIBLE : View.GONE);
        binding.kosongTeks.setText(
                hariIni ? R.string.m11_kosong_hari_ini : R.string.m11_kosong_riwayat);
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
