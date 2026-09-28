package id.lifeoffoods.ui.pesanan;

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
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.StatusPesanan;
import id.lifeoffoods.data.api.model.PesananBody;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.databinding.FragmentKodePickupBinding;
import id.lifeoffoods.databinding.IncludeBarisTiketBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;
import java.time.Instant;

/** K14 Kode pickup: kode 6 karakter untuk kasir (M12), rincian pesanan, dan dampaknya. */
public class KodePickupFragment extends Fragment {

    public static final String ARG_ID_PESANAN = "order_id";

    private FragmentKodePickupBinding binding;
    private KodePickupViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentKodePickupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(KodePickupViewModel.class);
        SisiAman.atasBawah(binding.isi);
        SisiAman.ikonGelap(requireActivity(), false);

        binding.barisMitra.label.setText(R.string.k14_mitra);
        binding.barisJam.label.setText(R.string.k12_jam_ambil);
        binding.barisJumlah.label.setText(R.string.k14_jumlah);
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muatUlang());
        binding.tombolPesananSaya.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.k15_pesanan));
        binding.tombolCariLagi.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack(R.id.k07_beranda, false));

        vm.muat(requireArguments().getLong(ARG_ID_PESANAN));
        vm.status.observe(getViewLifecycleOwner(), s -> tampilkan());
        vm.pesanan.observe(getViewLifecycleOwner(), p -> tampilkan());
        vm.sesiBerakhir.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        ((MainActivity) requireActivity()).sesiBerakhir();
                    }
                });
    }

    private void tampilkan() {
        KodePickupViewModel.Status s = vm.status.getValue();
        PesananDto p = vm.pesanan.getValue();
        boolean siap = s == KodePickupViewModel.Status.SIAP && p != null;
        binding.kartu.setVisibility(siap ? View.VISIBLE : View.INVISIBLE);
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == KodePickupViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == KodePickupViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        binding.dampak.setVisibility(siap ? View.VISIBLE : View.GONE);
        if (!siap) {
            binding.pilStatus.setVisibility(View.INVISIBLE);
            return;
        }

        StatusPesanan.Jenis jenis = StatusPesanan.jenis(p.status, p.pickupStart, Instant.now());
        binding.pilStatus.setVisibility(View.VISIBLE);
        binding.status.setText(StatusPesanan.label(jenis));
        binding.titikStatus.setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                requireContext(),
                                jenis == StatusPesanan.Jenis.SIAP
                                        ? R.color.merek_lime
                                        : jenis == StatusPesanan.Jenis.MENUNGGU
                                                ? R.color.tanda_bintang
                                                : R.color.ikon_redup)));

        boolean adaKode = p.pickupCode != null && !p.pickupCode.isEmpty();
        binding.kode.setText(adaKode ? p.pickupCode : "------");
        binding.kode.setContentDescription(
                adaKode ? getString(R.string.k14_kode_eja, eja(p.pickupCode)) : null);
        binding.petunjuk.setText(
                adaKode ? R.string.k14_tunjukkan : R.string.k14_kode_tidak_berlaku);

        baris(binding.barisMitra, p.store == null ? "" : p.store.name);
        baris(binding.barisJam, FormatTampilan.rentangJam(p.pickupStart, p.pickupEnd));
        baris(binding.barisJumlah, getString(R.string.k11_jumlah_item, p.itemCount));
        boolean lunas = "paid".equals(p.paymentStatus);
        binding.barisBayar.label.setText(
                lunas ? R.string.k14_dibayar : R.string.k14_bayar_di_tempat);
        baris(
                binding.barisBayar,
                FormatTampilan.rupiah(p.totalRupiah)
                        + (lunas
                                ? ""
                                : " · "
                                        + getString(
                                                PesananBody.BAYAR_QRIS.equals(p.paymentMethod)
                                                        ? R.string.k14_qris
                                                        : R.string.k14_tunai)));
        binding.barisBayar.nilai.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.teks_merek));
        binding.dampakTeks.setText(
                getResources()
                        .getQuantityString(
                                jenis == StatusPesanan.Jenis.SELESAI
                                        ? R.plurals.k14_dampak_selesai
                                        : R.plurals.k14_dampak,
                                p.itemCount,
                                p.itemCount));
    }

    private static void baris(IncludeBarisTiketBinding b, String nilai) {
        b.nilai.setText(nilai);
    }

    /** "LF7Q2K" dibacakan "L F 7 Q 2 K" oleh pembaca layar. */
    private static String eja(String kode) {
        StringBuilder sb = new StringBuilder();
        for (char c : kode.toCharArray()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
