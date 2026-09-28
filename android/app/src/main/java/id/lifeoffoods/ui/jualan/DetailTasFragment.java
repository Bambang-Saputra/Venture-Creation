package id.lifeoffoods.ui.jualan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.Kandungan;
import id.lifeoffoods.data.api.model.ListingDetailDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.databinding.FragmentDetailTasBinding;
import id.lifeoffoods.ui.umum.Pil;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;
import java.util.Set;

/** K10 Detail tas kejutan: isi, kandungan, peringatan alergi, jumlah tas, lalu K12 Ringkasan. */
public class DetailTasFragment extends Fragment {

    private FragmentDetailTasBinding binding;
    private DetailJualanViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailTasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(DetailJualanViewModel.class);
        SisiAman.atas(binding.tombolAtas);
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);

        binding.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());
        binding.tombolFavorit.setOnClickListener(v -> vm.ubahFavorit());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolPesan.setOnClickListener(
                v ->
                        BersamaDetail.lanjutKeRingkasan(
                                this, R.id.k12_ringkasan_tas, vm.keranjang.terpilih()));

        vm.muat(BersamaDetail.idJualan(this), false);
        vm.status.observe(getViewLifecycleOwner(), this::tampilkanStatus);
        vm.detail.observe(getViewLifecycleOwner(), this::isi);
        vm.jarakKm.observe(getViewLifecycleOwner(), km -> isiMeta());
        vm.alergiProfil.observe(getViewLifecycleOwner(), this::isiPeringatan);
        vm.menuToko.observe(getViewLifecycleOwner(), this::isiTautanMenu);
        vm.keranjangBerubah.observe(getViewLifecycleOwner(), n -> isiAksi());
        vm.favorit.observe(
                getViewLifecycleOwner(),
                fav -> {
                    binding.tombolFavorit.setImageTintList(
                            ContextCompat.getColorStateList(
                                    requireContext(), BersamaDetail.warnaFavorit(fav)));
                    binding.tombolFavorit.setContentDescription(
                            getString(BersamaDetail.keteranganFavorit(fav)));
                });
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.sesiBerakhir.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        BersamaDetail.sesiBerakhir(this);
                    }
                });
    }

    private void tampilkanStatus(DetailJualanViewModel.Status s) {
        boolean siap = s == DetailJualanViewModel.Status.SIAP;
        binding.rincian.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == DetailJualanViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == DetailJualanViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        binding.bilahAksi.setVisibility(siap ? View.VISIBLE : View.GONE);
    }

    private void isi(@Nullable ListingDetailDto d) {
        if (d == null) {
            return;
        }
        binding.tombolFavorit.setVisibility(
                app().sesi().sudahMasuk() && d.store != null ? View.VISIBLE : View.GONE);
        binding.judul.setText(d.title);
        isiMeta();

        binding.harga.setText(FormatTampilan.rupiah(d.priceRupiah));
        boolean adaNilai = d.originalValueRupiah != null && d.originalValueRupiah > d.priceRupiah;
        binding.nilai.setVisibility(adaNilai ? View.VISIBLE : View.GONE);
        if (adaNilai) {
            binding.nilai.setText(
                    getString(R.string.k10_nilai, FormatTampilan.rupiah(d.originalValueRupiah)));
            binding.nilai.setPaintFlags(
                    binding.nilai.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
        int hemat = FormatTampilan.persenHemat(d.priceRupiah, d.originalValueRupiah);
        binding.hemat.setVisibility(hemat > 0 ? View.VISIBLE : View.GONE);
        binding.hemat.setText(getString(R.string.k10_hemat, hemat));
        Pil.ikon(binding.chipJenis, R.drawable.ic_paket, R.color.teks_utama);

        binding.info.removeAllViews();
        BersamaDetail.chipAmbil(binding.info, d);
        BersamaDetail.chip(
                binding.info,
                getString(R.string.k10_sisa_tas, d.qtyRemaining),
                R.drawable.bg_pil_isian,
                R.color.teks_kuat,
                R.drawable.ic_paket);

        isiKandungan(d);
        isiPerkiraan(d);
        isiPeringatan(vm.alergiProfil.getValue());
        isiAksi();
    }

    /** "Kopi Kalyan · 380 m · tutup 21.00"; bagian yang datanya tidak ada dilewati. */
    private void isiMeta() {
        ListingDetailDto d = vm.detail.getValue();
        if (d == null || d.store == null) {
            return;
        }
        StringBuilder sb = new StringBuilder(d.store.name);
        String jarak = FormatTampilan.jarak(vm.jarakKm.getValue());
        if (jarak != null) {
            sb.append(" · ").append(jarak);
        }
        ListingDto.JamHariIni jam = d.store.hoursToday;
        if (jam != null) {
            String tutup = FormatTampilan.jamToko(jam.closeTime);
            if (jam.isClosed == 1) {
                sb.append(" · ").append(getString(R.string.k10_tutup_hari_ini));
            } else if (!tutup.isEmpty()) {
                sb.append(" · ").append(getString(R.string.k10_tutup, tutup));
            }
        }
        binding.meta.setText(sb);
    }

    private void isiKandungan(ListingDetailDto d) {
        binding.label.removeAllViews();
        if ("certified".equals(d.halalLabel) || "self_claim".equals(d.halalLabel)) {
            BersamaDetail.chip(
                    binding.label,
                    getString(
                            "certified".equals(d.halalLabel)
                                    ? R.string.halal_bersertifikat
                                    : R.string.halal_klaim),
                    R.drawable.bg_pil_merek,
                    R.color.teks_merek_gelap,
                    R.drawable.ic_perisai);
        }
        if (d.allergens != null) {
            for (ListingDto.Alergen a : d.allergens) {
                boolean mungkin = ListingDto.Alergen.MUNGKIN.equals(a.presence);
                BersamaDetail.chip(
                        binding.label,
                        mungkin ? getString(R.string.alergen_mungkin, a.name) : a.name,
                        R.drawable.bg_pil_isian,
                        R.color.teks_kuat,
                        0);
            }
        }
        String bahan = d.ingredientsText;
        boolean adaBahan = bahan != null && !bahan.trim().isEmpty();
        binding.bahan.setVisibility(
                adaBahan || binding.label.getChildCount() == 0 ? View.VISIBLE : View.GONE);
        binding.bahan.setText(
                adaBahan
                        ? getString(R.string.k10_bahan, bahan.trim())
                        : getString(R.string.k10_tanpa_alergen));
        binding.label.setVisibility(binding.label.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void isiPerkiraan(ListingDetailDto d) {
        String teks =
                d.description != null && !d.description.trim().isEmpty()
                        ? d.description
                        : d.contentHint;
        binding.deskripsi.setText(
                teks == null || teks.trim().isEmpty()
                        ? getString(R.string.k10_isi_belum_ditulis)
                        : teks.trim());
        binding.daftarIsi.removeAllViews();
        List<ListingDetailDto.Item> items = d.items;
        if (items == null) {
            return;
        }
        LayoutInflater inf = getLayoutInflater();
        for (ListingDetailDto.Item it : items) {
            View baris = inf.inflate(R.layout.item_butir_isi, binding.daftarIsi, false);
            TextView label = baris.findViewById(R.id.label);
            label.setText(it.qty != null && it.qty > 1 ? it.qty + " " + it.label : it.label);
            binding.daftarIsi.addView(baris);
        }
    }

    private void isiPeringatan(@Nullable Set<String> profil) {
        ListingDetailDto d = vm.detail.getValue();
        String teks = d == null ? null : Kandungan.peringatan(d.allergens, profil);
        binding.peringatan.setVisibility(teks == null ? View.GONE : View.VISIBLE);
        binding.peringatanTeks.setText(teks);
    }

    private void isiTautanMenu(@Nullable List<ListingDto> menu) {
        ListingDetailDto d = vm.detail.getValue();
        boolean ada = menu != null && !menu.isEmpty() && d != null && d.store != null;
        binding.tautanMenu.setVisibility(ada ? View.VISIBLE : View.GONE);
        if (!ada) {
            return;
        }
        binding.tautanMenuTeks.setText(getString(R.string.k10_lihat_menu, d.store.name));
        long pertama = menu.get(0).id;
        binding.tautanMenu.setOnClickListener(
                v -> {
                    Bundle args = new Bundle();
                    args.putLong(BersamaDetail.ARG_LISTING_ID, pertama);
                    NavHostFragment.findNavController(this).navigate(R.id.k11_detail_menu, args);
                });
    }

    private void isiAksi() {
        ListingDetailDto d = vm.detail.getValue();
        if (d == null) {
            return;
        }
        int qty = vm.keranjang.qty(d.id);
        binding.jumlahRingkas.setText(getString(R.string.k10_jumlah_tas, Math.max(qty, 1)));
        binding.total.setText(FormatTampilan.rupiah(d.priceRupiah * Math.max(qty, 1)));
        Stepper.isi(
                binding.stepper,
                Math.max(qty, 1),
                d.isAvailable && vm.keranjang.bisaKurang(d.id, vm.minimal()),
                d.isAvailable && vm.keranjang.bisaTambah(d.id),
                () -> vm.kurang(d.id),
                () -> vm.tambah(d.id));
        binding.tombolPesan.setEnabled(d.isAvailable && qty > 0);
        binding.tombolPesan.setText(d.isAvailable ? R.string.k10_pesan : R.string.k10_habis);
    }

    private LofApp app() {
        return (LofApp) requireActivity().getApplication();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
