package id.lifeoffoods.ui.pesanan;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.RingkasanPesanan;
import id.lifeoffoods.data.api.model.PesananBody;
import id.lifeoffoods.data.api.model.PratinjauPesananDto;
import id.lifeoffoods.databinding.FragmentRingkasanPesananBinding;
import id.lifeoffoods.databinding.ItemBarisRingkasanBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.jualan.Stepper;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.ArrayList;
import java.util.List;

/**
 * K12 Ringkasan tas kejutan dan K13 Ringkasan menu satuan (PRD-06). Argumen dari K10/K11: {@code
 * id_jualan} (long[]), {@code jumlah} (int[]), opsional {@code stok} (int[]) dan {@code
 * harga_normal} (long[]) dengan urutan yang sama, serta {@code menu_satuan} dari graf navigasi.
 */
public class RingkasanPesananFragment extends Fragment {

    public static final String ARG_ID_JUALAN = "id_jualan";
    public static final String ARG_JUMLAH = "jumlah";
    public static final String ARG_STOK = "stok";
    public static final String ARG_HARGA_NORMAL = "harga_normal";
    public static final String ARG_MENU_SATUAN = "menu_satuan";

    /** Kunci SavedStateHandle K10/K11: true berarti detail perlu dimuat ulang (stok berubah). */
    public static final String HASIL_MUAT_ULANG = "ringkasan_muat_ulang";

    private FragmentRingkasanPesananBinding binding;
    private RingkasanPesananViewModel vm;
    private BaseListAdapter<PratinjauPesananDto.Baris, ItemBarisRingkasanBinding> adapter;
    private String metodeBayar = PesananBody.BAYAR_TUNAI;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRingkasanPesananBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(RingkasanPesananViewModel.class);
        Bundle args = requireArguments();
        long[] id = args.getLongArray(ARG_ID_JUALAN);
        int[] qty = args.getIntArray(ARG_JUMLAH);
        if (id == null || qty == null || id.length == 0 || id.length != qty.length) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        vm.mulai(id, qty, args.getIntArray(ARG_STOK), args.getBoolean(ARG_MENU_SATUAN, false));

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.bilahAksi);
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.k12_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tabKurir.setOnClickListener(
                v -> Snackbar.make(view, R.string.k12_kurir_belum, Snackbar.LENGTH_LONG).show());

        long[] normal = args.getLongArray(ARG_HARGA_NORMAL);
        adapter =
                new BaseListAdapter<>(
                        ItemBarisRingkasanBinding::inflate,
                        (b, baris) -> isiBaris(b, baris, id, normal),
                        baris -> baris.listingId);
        binding.daftarItem.setAdapter(adapter);

        binding.barisTunai.setOnClickListener(v -> pilihBayar(PesananBody.BAYAR_TUNAI));
        binding.radioTunai.setOnClickListener(v -> pilihBayar(PesananBody.BAYAR_TUNAI));
        binding.barisQris.setOnClickListener(v -> pilihBayar(PesananBody.BAYAR_QRIS));
        binding.radioQris.setOnClickListener(v -> pilihBayar(PesananBody.BAYAR_QRIS));
        if (savedInstanceState != null) {
            metodeBayar = savedInstanceState.getString("metode_bayar", PesananBody.BAYAR_TUNAI);
        }
        pilihBayar(metodeBayar);

        binding.tombolBuat.setOnClickListener(
                v -> {
                    CharSequence c = binding.catatan.getText();
                    vm.buat(c == null ? null : c.toString(), metodeBayar);
                });

        vm.status.observe(getViewLifecycleOwner(), this::tampilkanStatus);
        vm.pratinjau.observe(getViewLifecycleOwner(), this::isi);
        vm.membuat.observe(
                getViewLifecycleOwner(),
                jalan -> {
                    binding.tombolBuat.setEnabled(!jalan);
                    binding.tombolBuat.setText(
                            jalan ? R.string.k12_membuat : R.string.k12_buat_pesanan);
                });
        vm.galat.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        Snackbar.make(view, pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
        vm.keDetail.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        kembaliKeDetail(pesan);
                    }
                });
        vm.kePesananSaya.observe(
                getViewLifecycleOwner(),
                p -> {
                    String pesan = p.ambil();
                    if (pesan != null) {
                        pesanDiAktivitas(pesan);
                        NavHostFragment.findNavController(this).navigate(R.id.k15_pesanan);
                    }
                });
        vm.selesai.observe(
                getViewLifecycleOwner(),
                p -> {
                    Long idPesanan = p.ambil();
                    if (idPesanan != null) {
                        keKodePickup(idPesanan);
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

    private void tampilkanStatus(RingkasanPesananViewModel.Status s) {
        boolean siap = s == RingkasanPesananViewModel.Status.SIAP;
        binding.rincian.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == RingkasanPesananViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == RingkasanPesananViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        binding.bilahAksi.setVisibility(siap ? View.VISIBLE : View.GONE);
    }

    private void isi(@Nullable PratinjauPesananDto p) {
        if (p == null) {
            return;
        }
        if (p.store != null) {
            // Figma hanya menulis alamat; kalau toko belum mengisi alamat, pakai namanya.
            boolean adaAlamat = p.store.address != null && !p.store.address.trim().isEmpty();
            binding.alamat.setText(adaAlamat ? p.store.address : p.store.name);
        }
        binding.jamAmbil.setText(FormatTampilan.rentangJam(p.pickupStart, p.pickupEnd));
        int jumlahItem = p.items == null ? 0 : p.items.size();
        if (vm.menuSatuan()) {
            binding.judulItem.setText(getString(R.string.k12_item, jumlahItem));
        } else {
            binding.judulItem.setText(R.string.k12_pesanan);
        }
        // Salinan baru supaya ListAdapter menggambar ulang stepper walau id barisnya sama.
        adapter.submitList(p.items == null ? null : new ArrayList<>(p.items));
        adapter.notifyItemRangeChanged(0, jumlahItem);
        isiPeringatan(p);
        binding.total.setText(FormatTampilan.rupiah(p.totalRupiah));
    }

    private void isiBaris(
            ItemBarisRingkasanBinding b,
            PratinjauPesananDto.Baris baris,
            long[] id,
            @Nullable long[] normal) {
        b.judul.setText(baris.title);
        boolean menu = vm.menuSatuan();
        b.ikon.setImageResource(menu ? R.drawable.ic_tas : R.drawable.ic_gambar);
        b.ikon.setImageTintList(
                ContextCompat.getColorStateList(
                        requireContext(), menu ? R.color.merek_utama : R.color.ikon_redup));
        b.keterangan.setVisibility(menu ? View.GONE : View.VISIBLE);
        b.barisHarga.setVisibility(menu ? View.VISIBLE : View.GONE);
        b.jumlahHarga.setVisibility(menu ? View.GONE : View.VISIBLE);
        b.stepper.getRoot().setVisibility(menu ? View.VISIBLE : View.GONE);
        if (!menu) {
            b.jumlahHarga.setText(
                    getString(
                            R.string.k12_jumlah_harga,
                            baris.qty,
                            FormatTampilan.rupiah(baris.unitPriceRupiah)));
            return;
        }
        b.harga.setText(FormatTampilan.rupiah(baris.unitPriceRupiah));
        Long asli = hargaNormal(baris.listingId, id, normal);
        boolean coret = asli != null && asli > baris.unitPriceRupiah;
        b.hargaNormal.setVisibility(coret ? View.VISIBLE : View.GONE);
        if (coret) {
            b.hargaNormal.setText(FormatTampilan.rupiah(asli));
            b.hargaNormal.setPaintFlags(
                    b.hargaNormal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        long l = baris.listingId;
        Stepper.isi(
                b.stepper,
                vm.keranjang.qty(l),
                vm.keranjang.bisaKurang(l, 1),
                vm.keranjang.bisaTambah(l),
                () -> vm.ubah(l, false),
                () -> vm.ubah(l, true));
    }

    @Nullable
    private static Long hargaNormal(long listingId, long[] id, @Nullable long[] normal) {
        if (normal == null) {
            return null;
        }
        for (int i = 0; i < id.length && i < normal.length; i++) {
            if (id[i] == listingId && normal[i] > 0) {
                return normal[i];
            }
        }
        return null;
    }

    private void isiPeringatan(PratinjauPesananDto p) {
        List<RingkasanPesanan.Peringatan> daftar = RingkasanPesanan.peringatan(p);
        binding.peringatan.setVisibility(daftar.isEmpty() ? View.GONE : View.VISIBLE);
        List<String> baris = new ArrayList<>();
        for (RingkasanPesanan.Peringatan w : daftar) {
            String t =
                    getString(
                            w.mungkin
                                    ? R.string.k12_peringatan_baris_mungkin
                                    : R.string.k12_peringatan_baris,
                            w.judul,
                            w.alergen);
            if (w.berat) {
                t = t + " " + getString(R.string.k12_peringatan_berat);
            }
            baris.add(t);
        }
        binding.isiPeringatan.setText(TextUtils.join("\n", baris));
    }

    private void pilihBayar(String metode) {
        metodeBayar = metode;
        binding.radioTunai.setChecked(PesananBody.BAYAR_TUNAI.equals(metode));
        binding.radioQris.setChecked(PesananBody.BAYAR_QRIS.equals(metode));
    }

    /** Stok berubah: detail di bawahnya dimuat ulang, pesan tetap terlihat sesudah kembali. */
    private void kembaliKeDetail(String pesan) {
        NavController nav = NavHostFragment.findNavController(this);
        NavBackStackEntry sebelum = nav.getPreviousBackStackEntry();
        if (sebelum != null) {
            sebelum.getSavedStateHandle().set(HASIL_MUAT_ULANG, true);
        }
        pesanDiAktivitas(pesan);
        nav.popBackStack();
    }

    /** K14 menggantikan K10/K11 dan ringkasan, jadi Kembali dari K14 menuju beranda. */
    private void keKodePickup(long idPesanan) {
        Bundle a = new Bundle();
        a.putLong(KodePickupFragment.ARG_ID_PESANAN, idPesanan);
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.k07_beranda, false).build();
        NavHostFragment.findNavController(this).navigate(R.id.k14_kode_pickup, a, opsi);
    }

    /** Snackbar di view aktivitas supaya tidak ikut hilang saat fragment ini ditutup. */
    private void pesanDiAktivitas(String pesan) {
        Snackbar.make(requireActivity().findViewById(android.R.id.content), pesan, 5000).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("metode_bayar", metodeBayar);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.daftarItem.setAdapter(null);
        }
        binding = null;
    }
}
