package id.lifeoffoods.ui.jualan;

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
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.Kandungan;
import id.lifeoffoods.data.api.model.ListingDetailDto;
import id.lifeoffoods.data.api.model.ListingDto;
import id.lifeoffoods.databinding.FragmentMenuSatuanBinding;
import id.lifeoffoods.databinding.ItemBarisMenuBinding;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.Pil;
import id.lifeoffoods.ui.umum.SisiAman;

/**
 * K11 Detail menu satuan: semua menu satuan toko dari jualan yang dibuka, dipilih dengan stepper,
 * lalu K13 Ringkasan. Jualan yang dibuka berada paling atas dan langsung berisi 1.
 */
public class MenuSatuanFragment extends Fragment {

    private FragmentMenuSatuanBinding binding;
    private DetailJualanViewModel vm;
    private BaseListAdapter<ListingDto, ItemBarisMenuBinding> adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMenuSatuanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(DetailJualanViewModel.class);
        SisiAman.atas(binding.tombolAtas);
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);

        adapter = new BaseListAdapter<>(ItemBarisMenuBinding::inflate, this::isiBaris, l -> l.id);
        binding.daftarMenu.setAdapter(adapter);

        binding.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());
        binding.tombolFavorit.setOnClickListener(v -> vm.ubahFavorit());
        binding.tombolMaps.setOnClickListener(
                v -> {
                    ListingDetailDto d = vm.detail.getValue();
                    BersamaDetail.bukaMaps(this, d == null ? null : d.store);
                });
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolLanjut.setOnClickListener(
                v ->
                        BersamaDetail.lanjutKeRingkasan(
                                this,
                                R.id.k13_ringkasan_menu,
                                vm.keranjang.terpilih(),
                                vm.menuToko.getValue()));
        BersamaDetail.muatUlangSetelahRingkasan(this, vm::muatUlang);
        Pil.ikon(binding.chipJenis, R.drawable.ic_tas, R.color.teks_kuat);

        vm.muat(BersamaDetail.idJualan(this), true);
        vm.status.observe(getViewLifecycleOwner(), this::tampilkanStatus);
        vm.detail.observe(getViewLifecycleOwner(), this::isiToko);
        vm.jarakKm.observe(getViewLifecycleOwner(), km -> isiInfo());
        vm.menuToko.observe(getViewLifecycleOwner(), adapter::submitList);
        // Profil bisa termuat setelah daftar menu; baris digambar ulang supaya tanda alergi muncul.
        vm.alergiProfil.observe(
                getViewLifecycleOwner(),
                p -> adapter.notifyItemRangeChanged(0, adapter.getItemCount()));
        vm.keranjangBerubah.observe(
                getViewLifecycleOwner(),
                n -> {
                    // Batas 10 item per pesanan bisa mengubah tombol tambah di baris lain.
                    adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                    isiAksi();
                });
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

    private void isiToko(@Nullable ListingDetailDto d) {
        if (d == null || d.store == null) {
            return;
        }
        LofApp app = (LofApp) requireActivity().getApplication();
        binding.tombolFavorit.setVisibility(app.sesi().sudahMasuk() ? View.VISIBLE : View.GONE);
        binding.namaToko.setText(d.store.name);
        String alamat = d.store.address;
        binding.alamat.setVisibility(
                alamat == null || alamat.trim().isEmpty() ? View.GONE : View.VISIBLE);
        binding.alamat.setText(alamat);
        binding.tombolMaps.setVisibility(
                BersamaDetail.adaLokasi(d.store) ? View.VISIBLE : View.GONE);
        binding.ikonKategori.setImageResource(BersamaDetail.ikonKategori(d.store.category));
        binding.judul.setText(getString(R.string.k11_judul, d.store.name));
        isiInfo();
        isiAksi();
    }

    private void isiInfo() {
        ListingDetailDto d = vm.detail.getValue();
        if (d == null) {
            return;
        }
        binding.info.removeAllViews();
        BersamaDetail.chipRating(binding.info, d);
        BersamaDetail.chipAmbil(binding.info, d);
        String jarak = FormatTampilan.jarak(vm.jarakKm.getValue());
        if (jarak != null) {
            BersamaDetail.chip(
                    binding.info,
                    jarak,
                    R.drawable.bg_pil_isian,
                    R.color.teks_kuat,
                    R.drawable.ic_pin_lokasi);
        }
    }

    private void isiBaris(ItemBarisMenuBinding b, ListingDto l) {
        b.judul.setText(l.title);
        String kandungan = Kandungan.ringkas(l.allergens);
        b.kandungan.setText(
                kandungan.isEmpty() ? getString(R.string.k11_tanpa_alergen) : kandungan);
        b.kandungan.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        Kandungan.hanyaMungkin(l.allergens)
                                ? R.color.tanda_proses_teks
                                : R.color.teks_sekunder));
        String cocok = Kandungan.cocokProfil(l.allergens, vm.alergiProfil.getValue());
        b.peringatan.setVisibility(cocok == null ? View.GONE : View.VISIBLE);
        b.peringatan.setText(cocok);
        // Daftar /listings tidak membawa nomor sertifikat; teksnya tetap tidak pernah "halal" saja.
        b.halal.setText(Kandungan.labelHalal(l.halalLabel, null));
        b.harga.setText(FormatTampilan.rupiah(l.priceRupiah));
        Long asli = l.originalValueRupiah;
        boolean coret = asli != null && asli > l.priceRupiah;
        b.hargaNormal.setVisibility(coret ? View.VISIBLE : View.GONE);
        if (coret) {
            b.hargaNormal.setText(FormatTampilan.rupiah(asli));
            b.hargaNormal.setPaintFlags(
                    b.hargaNormal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        b.stok.setText(
                l.qtyRemaining > 0
                        ? getString(R.string.k11_sisa, l.qtyRemaining)
                        : getString(R.string.k11_habis));
        Stepper.isi(
                b.stepper,
                vm.keranjang.qty(l.id),
                vm.keranjang.bisaKurang(l.id, 0),
                vm.keranjang.bisaTambah(l.id),
                () -> vm.kurang(l.id),
                () -> vm.tambah(l.id));
    }

    private void isiAksi() {
        int n = vm.keranjang.totalQty();
        binding.jumlahRingkas.setText(getString(R.string.k11_jumlah_item, n));
        binding.total.setText(FormatTampilan.rupiah(vm.keranjang.totalRupiah()));
        binding.tombolLanjut.setEnabled(n > 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.daftarMenu.setAdapter(null);
        binding = null;
    }
}
