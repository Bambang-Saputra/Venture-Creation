package id.lifeoffoods.ui.mitra;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.JualanMitra;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.databinding.FragmentKelolaJualanBinding;
import id.lifeoffoods.databinding.ItemJualanMitraBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.BaseListAdapter;
import id.lifeoffoods.ui.umum.FotoJualan;
import id.lifeoffoods.ui.umum.FotoUnggah;
import id.lifeoffoods.ui.umum.Pil;
import id.lifeoffoods.ui.umum.SisiAman;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * M10 Kelola jualan dan M17 Kelola menu satuan (PRD-09). Argumen {@code tipe} (surprise_bag atau
 * menu_item) memilih segmen awal. Dimuat ulang tiap layar terlihat, jadi jualan yang baru
 * diterbitkan di M09/M16 langsung tampil.
 */
public class KelolaJualanFragment extends Fragment {

    public static final String ARG_TIPE = "tipe";

    private FragmentKelolaJualanBinding binding;

    /** Jualan yang slot fotonya diketuk, sampai pemilih foto kembali. */
    @Nullable private JualanMitraDto jualanFoto;

    private final ActivityResultLauncher<PickVisualMediaRequest> pilihFoto =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        JualanMitraDto j = jualanFoto;
                        jualanFoto = null;
                        if (uri == null || j == null || binding == null) {
                            return;
                        }
                        FotoUnggah.baca(
                                this,
                                uri,
                                jpeg -> {
                                    if (jpeg == null) {
                                        Snackbar.make(
                                                        binding.getRoot(),
                                                        R.string.foto_gagal_dibaca,
                                                        Snackbar.LENGTH_LONG)
                                                .show();
                                    } else {
                                        this.vm.unggahFoto(j, jpeg);
                                    }
                                });
                    });
    private KelolaJualanViewModel vm;
    private BaseListAdapter<JualanMitraDto, ItemJualanMitraBinding> adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentKelolaJualanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(KelolaJualanViewModel.class);
        Bundle args = getArguments();
        vm.mulai(
                args == null
                        ? JualanMitraDto.TIPE_TAS
                        : args.getString(ARG_TIPE, JualanMitraDto.TIPE_TAS));

        SisiAman.atas(binding.kepala.getRoot());
        SisiAman.bawah(binding.getRoot());
        SisiAman.ikonGelap(requireActivity(), true);
        binding.kepala.judul.setText(R.string.m10_judul);
        binding.kepala.tombolKembali.setOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());
        binding.keadaan.tombolCobaLagi.setOnClickListener(v -> vm.muat());
        binding.segmen.tabTas.setOnClickListener(v -> vm.pilihTipe(JualanMitraDto.TIPE_TAS));
        binding.segmen.tabMenu.setOnClickListener(v -> vm.pilihTipe(JualanMitraDto.TIPE_MENU));
        binding.tombolPasang.setOnClickListener(
                v ->
                        NavHostFragment.findNavController(this)
                                .navigate(menu() ? R.id.m16_pasang_menu : R.id.m09_pasang_tas));

        adapter = new BaseListAdapter<>(ItemJualanMitraBinding::inflate, this::isiKartu, j -> j.id);
        binding.daftar.setAdapter(adapter);

        vm.tipe.observe(getViewLifecycleOwner(), t -> tampilkanTipe());
        vm.daftar.observe(getViewLifecycleOwner(), d -> tampilkanDaftar());
        vm.status.observe(getViewLifecycleOwner(), s -> tampilkanDaftar());
        vm.sibuk.observe(
                getViewLifecycleOwner(),
                s -> adapter.notifyItemRangeChanged(0, adapter.getItemCount()));
        vm.namaAlergen.observe(
                getViewLifecycleOwner(),
                m -> adapter.notifyItemRangeChanged(0, adapter.getItemCount()));
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

    @Override
    public void onStart() {
        super.onStart();
        vm.muat();
    }

    private boolean menu() {
        return JualanMitraDto.TIPE_MENU.equals(vm.tipe.getValue());
    }

    private void tampilkanTipe() {
        boolean menu = menu();
        binding.segmen.tabTas.setSelected(!menu);
        binding.segmen.tabMenu.setSelected(menu);
        binding.teksTips.setText(menu ? R.string.m10_tips_menu : R.string.m10_tips_tas);
        binding.tombolPasang.setText(menu ? R.string.m10_tambah_item : R.string.m10_pasang_tas);
        binding.kosong.setText(menu ? R.string.m10_kosong_menu : R.string.m10_kosong_tas);
    }

    private void tampilkanDaftar() {
        KelolaJualanViewModel.Status s = vm.status.getValue();
        boolean siap = s == KelolaJualanViewModel.Status.SIAP;
        binding.keadaan.getRoot().setVisibility(siap ? View.GONE : View.VISIBLE);
        binding.keadaan.progres.setVisibility(
                s == KelolaJualanViewModel.Status.MEMUAT ? View.VISIBLE : View.GONE);
        binding.keadaan.gagal.setVisibility(
                s == KelolaJualanViewModel.Status.GAGAL ? View.VISIBLE : View.GONE);
        if (s == KelolaJualanViewModel.Status.GAGAL && vm.galatAwal.getValue() != null) {
            // Pesan server, misalnya penolakan sopan untuk kasir (PRD-09 kriteria 7).
            binding.keadaan.gagalIsi.setText(vm.galatAwal.getValue());
        }
        List<JualanMitraDto> d = vm.daftar.getValue();
        adapter.submitList(siap ? d : null);
        binding.kosong.setVisibility(siap && (d == null || d.isEmpty()) ? View.VISIBLE : View.GONE);
    }

    private void isiKartu(ItemJualanMitraBinding b, JualanMitraDto j) {
        b.judul.setText(j.title);
        b.hargaJam.setText(
                getString(
                        R.string.m10_harga_jam,
                        FormatTampilan.rupiah(j.priceRupiah),
                        FormatTampilan.rentangJam(j.pickupStart, j.pickupEnd)));
        b.sisa.setText(
                JualanMitraDto.TIPE_MENU.equals(j.type)
                        ? getString(R.string.m10_terjual, j.qtySold, j.qtyTotal)
                        : getString(R.string.m10_sisa, j.qtyRemaining, j.qtyTotal));

        b.label.removeAllViews();
        b.label.addView(pilStatus(requireContext(), j.status));
        if (JualanMitraDto.TIPE_MENU.equals(j.type) && j.allergens != null) {
            Map<String, String> nama = vm.namaAlergen.getValue();
            for (JualanMitraDto.Alergen a : j.allergens) {
                String n = nama == null || !nama.containsKey(a.code) ? a.code : nama.get(a.code);
                boolean mungkin = JualanMitraDto.Alergen.MUNGKIN.equals(a.presence);
                b.label.addView(
                        Pil.buat(
                                requireContext(),
                                mungkin ? getString(R.string.m10_alergen_mungkin, n) : n,
                                mungkin ? R.drawable.bg_pil_proses : R.drawable.bg_pil_isian,
                                mungkin ? R.color.tanda_proses_teks : R.color.teks_kuat,
                                0));
            }
        }

        Set<Long> sibuk = vm.sibuk.getValue();
        boolean diproses = sibuk != null && sibuk.contains(j.id);
        b.saklar.setOnCheckedChangeListener(null);
        // Selama diproses, biarkan saklar di posisi yang dipilih mitra; jawaban server yang
        // menentukan posisi akhirnya.
        if (!diproses) {
            b.saklar.setChecked(JualanMitra.saklarNyala(j.status));
        }
        b.saklar.setEnabled(!diproses && JualanMitra.saklarBisaDiubah(j.status));
        b.saklar.setContentDescription(getString(R.string.m10_saklar, j.title));
        b.saklar.setOnCheckedChangeListener((s, nyala) -> vm.ubahSaklar(j, nyala));

        FotoJualan.muat(b.fotoJualan, j.photoUrl, 12);
        b.progresFoto.setVisibility(diproses ? View.VISIBLE : View.GONE);
        b.slotFoto.setContentDescription(getString(R.string.m10_ganti_foto, j.title));
        b.slotFoto.setOnClickListener(
                v -> {
                    if (diproses) {
                        return;
                    }
                    jualanFoto = j;
                    pilihFoto.launch(
                            new PickVisualMediaRequest.Builder()
                                    .setMediaType(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    .INSTANCE)
                                    .build());
                });
    }

    /** Pil status jualan; juga dipakai daftar "Tas aktif hari ini" di M05. */
    static android.widget.TextView pilStatus(android.content.Context ctx, String status) {
        int teks;
        if (JualanMitraDto.AKTIF.equals(status)) {
            teks = R.string.m10_status_aktif;
        } else if (JualanMitraDto.DIJEDA.equals(status)) {
            teks = R.string.m10_status_dijeda;
        } else if (JualanMitraDto.DRAF.equals(status)) {
            teks = R.string.m10_status_draf;
        } else if (JualanMitraDto.HABIS.equals(status)) {
            teks = R.string.m10_status_habis;
        } else {
            teks = R.string.m10_status_lewat;
        }
        JualanMitra.Nada n = JualanMitra.nada(status);
        return Pil.buat(
                ctx,
                ctx.getString(teks),
                n == JualanMitra.Nada.HIJAU
                        ? R.drawable.bg_pil_merek
                        : n == JualanMitra.Nada.KUNING
                                ? R.drawable.bg_pil_proses
                                : R.drawable.bg_pil_isian,
                n == JualanMitra.Nada.HIJAU
                        ? R.color.tanda_hemat_teks
                        : n == JualanMitra.Nada.KUNING
                                ? R.color.tanda_proses_teks
                                : R.color.teks_kuat,
                0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.daftar.setAdapter(null);
        binding = null;
    }
}
