package id.lifeoffoods.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import id.lifeoffoods.R;
import id.lifeoffoods.databinding.FragmentOnboardingBinding;
import id.lifeoffoods.databinding.ItemHalamanOnboardingBinding;
import id.lifeoffoods.ui.umum.LokasiPerangkat;
import id.lifeoffoods.ui.umum.SisiAman;

/** K06 Onboarding. Sekali sesudah K05; Lewati dan halaman terakhir membuka K07. */
public class OnboardingFragment extends Fragment {

    /** Kicker, judul, isi per halaman. */
    private static final int[][] HALAMAN = {
        {R.string.k06_kicker_1, R.string.k06_judul_1, R.string.k06_isi_1},
        {R.string.k06_kicker_2, R.string.k06_judul_2, R.string.k06_isi_2},
        {R.string.k06_kicker_3, R.string.k06_judul_3, R.string.k06_isi_3},
    };

    private FragmentOnboardingBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SisiAman.bawah(binding.bawah);
        // Foto sampai ke balik status bar; ikonnya gelap di atas latar terang.
        SisiAman.ikonGelap(requireActivity(), true);

        binding.halaman.setAdapter(new Penyedia());
        binding.halaman.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int posisi) {
                        tampilkan(posisi);
                    }
                });
        binding.tombolLewati.setOnClickListener(v -> keBeranda());
        binding.tombolLanjut.setOnClickListener(
                v -> {
                    int i = binding.halaman.getCurrentItem();
                    if (i < HALAMAN.length - 1) {
                        binding.halaman.setCurrentItem(i + 1);
                    } else if (LokasiPerangkat.diizinkan(requireContext())) {
                        keBeranda();
                    } else {
                        // Beranda tetap dibuka apa pun jawabannya; izin bisa diberi nanti di K08.
                        mintaIzin.launch(LokasiPerangkat.IZIN);
                    }
                });
        tampilkan(binding.halaman.getCurrentItem());
    }

    private final ActivityResultLauncher<String[]> mintaIzin =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    hasil -> {
                        if (binding != null) {
                            keBeranda();
                        }
                    });

    private void tampilkan(int posisi) {
        boolean terakhir = posisi >= HALAMAN.length - 1;
        binding.catatanLokasi.setVisibility(
                terakhir && !LokasiPerangkat.diizinkan(requireContext())
                        ? View.VISIBLE
                        : View.GONE);
        binding.tombolLanjut.setText(terakhir ? R.string.k06_mulai : R.string.k06_lanjut);
        binding.tombolLewati.setVisibility(terakhir ? View.GONE : View.VISIBLE);
        View[] titik = {binding.titik1, binding.titik2, binding.titik3};
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < titik.length; i++) {
            boolean aktif = i == posisi;
            titik[i].setBackgroundResource(
                    aktif ? R.drawable.bg_titik_aktif : R.drawable.bg_titik_pasif);
            ViewGroup.LayoutParams lp = titik[i].getLayoutParams();
            lp.width = Math.round((aktif ? 22 : 6) * d);
            titik[i].setLayoutParams(lp);
        }
        binding.titik.setContentDescription(
                getString(R.string.k06_halaman, posisi + 1, HALAMAN.length));
    }

    private void keBeranda() {
        NavOptions opsi = new NavOptions.Builder().setPopUpTo(R.id.k06_onboarding, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.k07_beranda, null, opsi);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private final class Penyedia extends RecyclerView.Adapter<Penyedia.Holder> {

        final class Holder extends RecyclerView.ViewHolder {
            final ItemHalamanOnboardingBinding b;

            Holder(ItemHalamanOnboardingBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(
                    ItemHalamanOnboardingBinding.inflate(
                            LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int posisi) {
            h.b.kicker.setText(teks(HALAMAN[posisi][0]));
            h.b.judul.setText(teks(HALAMAN[posisi][1]));
            h.b.isi.setText(teks(HALAMAN[posisi][2]));
        }

        @Override
        public int getItemCount() {
            return HALAMAN.length;
        }

        private String teks(@StringRes int id) {
            return getString(id);
        }
    }
}
