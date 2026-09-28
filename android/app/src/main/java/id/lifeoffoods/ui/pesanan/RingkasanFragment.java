package id.lifeoffoods.ui.pesanan;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.api.model.PesananBody;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.databinding.FragmentRingkasanBinding;
import id.lifeoffoods.databinding.IncludeBarisBiayaBinding;
import id.lifeoffoods.databinding.ItemRingkasanMenuBinding;
import id.lifeoffoods.databinding.ItemRingkasanTasBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.SisiAman;
import id.lifeoffoods.ui.umum.Stepper;
import java.util.List;

/**
 * K12 Ringkasan tas kejutan dan K13 Ringkasan menu satuan. Argumen {@code menu} membedakan
 * keduanya: K13 bisa mengubah jumlah per item, K12 menampilkan "1 × Rp18.000".
 */
public class RingkasanFragment extends Fragment {

    public static final String ARG_MENU = "menu";
    public static final String ARG_ID_JUALAN = "id_jualan";
    public static final String ARG_JUMLAH = "jumlah";

    private FragmentRingkasanBinding binding;
    private RingkasanViewModel vm;
    private boolean menu;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRingkasanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        menu = args.getBoolean(ARG_MENU, false);
        vm = new ViewModelProvider(this).get(RingkasanViewModel.class);
        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);

        binding.kepala.judul.setText(R.string.k12_judul_bar);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatPratinjau());

        RecyclerView.Adapter<?> adapter =
                menu
                        ? new BaseListAdapter<RingkasanViewModel.Baris, ItemRingkasanMenuBinding>(
                                ItemRingkasanMenuBinding::inflate, this::isiBarisMenu, b -> b.id)
                        : new BaseListAdapter<RingkasanViewModel.Baris, ItemRingkasanTasBinding>(
                                ItemRingkasanTasBinding::inflate, this::isiBarisTas, b -> b.id);
        binding.daftarItem.setAdapter(adapter);

        binding.biayaSubtotal.label.setText(R.string.k12_subtotal);
        binding.biayaLayanan.label.setText(R.string.k12_biaya_layanan);
        binding.biayaDiskon.label.setText(R.string.k12_diskon);
        binding.biayaDiskon.nilai.setTextColor(requireContext().getColor(R.color.teks_merek));

        binding.bayarTunai.setOnClickListener(v -> pilihBayar(PesananBody.BAYAR_TUNAI));
        binding.bayarQris.setOnClickListener(v -> pilihBayar(PesananBody.BAYAR_QRIS));
        tandaiBayar(vm.caraBayar());

        binding.tombolBuat.setOnClickListener(
                v ->
                        vm.buat(
                                binding.catatan.getText() == null
                                        ? null
                                        : binding.catatan.getText().toString()));

        vm.mulai(args.getLongArray(ARG_ID_JUALAN), args.getIntArray(ARG_JUMLAH));
        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.pratinjau.observe(getViewLifecycleOwner(), p -> tampilkan());
        vm.pesanGagal.observe(getViewLifecycleOwner(), p -> tampilkan());
        vm.mengirim.observe(getViewLifecycleOwner(), m -> tampilkan());
        vm.baris.observe(
                getViewLifecycleOwner(),
                b -> {
                    submit(adapter, b);
                    int n = vm.keranjang.totalQty();
                    binding.judulItem.setText(
                            menu
                                    ? getString(R.string.k13_judul_item, n)
                                    : getString(R.string.k12_pesanan));
                });
        vm.peringatan.observe(
                getViewLifecycleOwner(),
                t -> {
                    binding.peringatan.setVisibility(t == null ? View.GONE : View.VISIBLE);
                    binding.peringatanTeks.setText(t);
                });
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.dibuat.observe(
                getViewLifecycleOwner(),
                p -> {
                    Long id = p.ambil();
                    if (id != null) {
                        bukaKode(id);
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

    @SuppressWarnings("unchecked")
    private static void submit(RecyclerView.Adapter<?> adapter, List<RingkasanViewModel.Baris> b) {
        ((BaseListAdapter<RingkasanViewModel.Baris, ?>) adapter).submitList(b);
    }

    private void tampilkan() {
        RingkasanViewModel.Status s = vm.status.getValue();
        PesananDto p = vm.pratinjau.getValue();
        boolean siap = s == RingkasanViewModel.Status.SIAP;
        binding.rincian.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == RingkasanViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == RingkasanViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);

        String gagal = vm.pesanGagal.getValue();
        binding.pesanGagal.setVisibility(gagal == null ? View.GONE : View.VISIBLE);
        binding.pesanGagal.setText(gagal);

        if (p != null) {
            binding.namaToko.setText(p.store == null ? "" : p.store.name);
            String alamat = p.store == null ? null : p.store.address;
            binding.alamat.setVisibility(alamat == null ? View.GONE : View.VISIBLE);
            binding.alamat.setText(alamat);
            binding.jamAmbil.setText(FormatTampilan.rentangJam(p.pickupStart, p.pickupEnd));
            biaya(binding.biayaSubtotal, p.subtotalRupiah, false);
            biaya(binding.biayaLayanan, p.serviceFeeRupiah, false);
            biaya(binding.biayaDiskon, p.discountRupiah, true);
            binding.biayaDiskon
                    .getRoot()
                    .setVisibility(p.discountRupiah > 0 ? View.VISIBLE : View.GONE);
            binding.totalRincian.setText(FormatTampilan.rupiah(p.totalRupiah));
        }
        // Total di bilah bawah ikut jumlah sekarang; kalau semua item 0, totalnya 0.
        boolean kosong = vm.keranjang.totalQty() == 0;
        binding.total.setText(FormatTampilan.rupiah(p == null || kosong ? 0 : p.totalRupiah));
        binding.tombolBuat.setEnabled(
                siap
                        && p != null
                        && !kosong
                        && gagal == null
                        && !Boolean.TRUE.equals(vm.mengirim.getValue()));
    }

    private static void biaya(IncludeBarisBiayaBinding b, long nilai, boolean potongan) {
        b.nilai.setText((potongan ? "-" : "") + FormatTampilan.rupiah(nilai));
    }

    private void isiBarisTas(ItemRingkasanTasBinding b, RingkasanViewModel.Baris baris) {
        b.judul.setText(baris.judul);
        b.jumlah.setText(
                getString(R.string.k12_kali, baris.qty, FormatTampilan.rupiah(baris.harga)));
    }

    private void isiBarisMenu(ItemRingkasanMenuBinding b, RingkasanViewModel.Baris baris) {
        b.judul.setText(baris.judul);
        b.harga.setText(FormatTampilan.rupiah(baris.harga));
        b.hargaNormal.setVisibility(baris.hargaNormal > 0 ? View.VISIBLE : View.GONE);
        if (baris.hargaNormal > 0) {
            b.hargaNormal.setText(FormatTampilan.rupiah(baris.hargaNormal));
            b.hargaNormal.setPaintFlags(
                    b.hargaNormal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        Stepper.isi(
                b.stepper,
                baris.qty,
                vm.keranjang.bisaKurang(baris.id, 0),
                vm.keranjang.bisaTambah(baris.id),
                () -> vm.kurang(baris.id),
                () -> vm.tambah(baris.id));
    }

    private void pilihBayar(String cara) {
        vm.pilihBayar(cara);
        tandaiBayar(cara);
    }

    private void tandaiBayar(String cara) {
        binding.radioTunai.setChecked(PesananBody.BAYAR_TUNAI.equals(cara));
        binding.radioQris.setChecked(PesananBody.BAYAR_QRIS.equals(cara));
        // Tanpa animasi saat layar pertama tampil, supaya titik radio langsung terlihat.
        binding.radioTunai.jumpDrawablesToCurrentState();
        binding.radioQris.jumpDrawablesToCurrentState();
    }

    /** Ke K14. Ringkasan dan detail dilepas dari tumpukan, jadi kembali dari K14 ke beranda. */
    private void bukaKode(long idPesanan) {
        Bundle args = new Bundle();
        args.putLong(KodePickupFragment.ARG_ID_PESANAN, idPesanan);
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.k07_beranda, false).build();
        NavHostFragment.findNavController(this).navigate(R.id.k14_kode_pickup, args, opsi);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.daftarItem.setAdapter(null);
        binding = null;
    }
}
