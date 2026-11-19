package id.lifeoffoods.ui.voucher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.model.DaftarVoucherDto;
import id.lifeoffoods.data.api.model.VoucherDto;
import id.lifeoffoods.databinding.FragmentVoucherSayaBinding;
import id.lifeoffoods.databinding.ItemVoucherBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.SegarkanTarik;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.ArrayList;
import java.util.List;

/** K22 Voucher saya (ADR-0008): Gosok & Menang, voucher pop up, dan voucher milik pembeli. */
public class VoucherSayaFragment extends Fragment {

    private FragmentVoucherSayaBinding binding;
    private VoucherViewModel vm;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentVoucherSayaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(VoucherViewModel.class);
        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.k22_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        BaseListAdapter<VoucherDto, ItemVoucherBinding> adapter =
                new BaseListAdapter<>(ItemVoucherBinding::inflate, TampilanVoucher::isi, v -> v.id);
        binding.daftar.setAdapter(adapter);
        binding.kartuGosok.setOnClickListener(v -> GosokDialog.tampil(this, vm::muatUlang));
        SwipeRefreshLayout segarkan = SegarkanTarik.bungkus(binding.gulir, vm::muatUlang);

        vm.muatUlang();
        vm.status.observe(
                getViewLifecycleOwner(),
                s -> {
                    binding.progres.setVisibility(
                            s == VoucherViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
                    SegarkanTarik.selesai(segarkan, s);
                });
        vm.daftar.observe(
                getViewLifecycleOwner(),
                d -> {
                    List<VoucherDto> milik =
                            d == null || d.mine == null ? new ArrayList<>() : d.mine;
                    adapter.submitList(milik);
                    binding.kosong.setVisibility(milik.isEmpty() ? View.VISIBLE : View.GONE);
                    isiPromo(d);
                    binding.ketGosok.setText(
                            d == null || d.canScratchToday
                                    ? R.string.gosok_ajak
                                    : R.string.gosok_sudah_main);
                });
        vm.mengklaim.observe(
                getViewLifecycleOwner(), jalan -> binding.tombolKlaim.setEnabled(!jalan));
        vm.diklaim.observe(
                getViewLifecycleOwner(),
                p -> {
                    if (p.ambil() != null) {
                        Snackbar.make(view, R.string.k22_diklaim, Snackbar.LENGTH_LONG).show();
                    }
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
                        ((MainActivity) requireActivity()).sesiBerakhir();
                    }
                });
    }

    private void isiPromo(@Nullable DaftarVoucherDto d) {
        VoucherDto promo = d == null ? null : d.promo;
        binding.kotakPromo.setVisibility(promo == null ? View.GONE : View.VISIBLE);
        if (promo != null) {
            TampilanVoucher.isi(binding.kartuPromo, promo);
            binding.tombolKlaim.setOnClickListener(v -> vm.klaim(promo.id));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
