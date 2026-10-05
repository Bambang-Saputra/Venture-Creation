package id.lifeoffoods.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import id.lifeoffoods.R;
import id.lifeoffoods.databinding.FragmentOnboardingBinding;
import id.lifeoffoods.databinding.ItemHalamanOnboardingBinding;
import id.lifeoffoods.ui.MainActivity;
import id.lifeoffoods.ui.umum.SisiAman;

/**
 * K06 Onboarding. Tampil sekali per instalasi, sebelum K01 Pilih peran. Lewati dan halaman terakhir
 * membuka K01. Izin lokasi tidak diminta di sini; K08 Peta memintanya saat dibutuhkan.
 */
public class OnboardingFragment extends Fragment {

    /** Kicker, judul, isi per halaman. */
    private static final int[][] HALAMAN = {
        {R.string.k06_kicker_1, R.string.k06_judul_1, R.string.k06_isi_1},
        {R.string.k06_kicker_2, R.string.k06_judul_2, R.string.k06_isi_2},
        {R.string.k06_kicker_3, R.string.k06_judul_3, R.string.k06_isi_3},
    };

    /** Foto per halaman (CC BY/BY-SA, atribusi di docs/aset/SUMBER-foto-onboarding.md). */
    private static final int[] FOTO = {
        R.drawable.foto_perkenalan_bakery,
        R.drawable.foto_perkenalan_kacang,
        R.drawable.foto_perkenalan_kasir,
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
        // Foto sampai ke balik status bar dan bagian atasnya diberi selubung gelap: ikon terang.
        SisiAman.ikonGelap(requireActivity(), false);

        binding.halaman.setAdapter(new Penyedia());
        binding.halaman.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int posisi) {
                        tampilkan(posisi);
                    }
                });
        binding.tombolLewati.setOnClickListener(v -> selesai());
        binding.tombolLanjut.setOnClickListener(
                v -> {
                    int i = binding.halaman.getCurrentItem();
                    if (i < HALAMAN.length - 1) {
                        binding.halaman.setCurrentItem(i + 1);
                    } else {
                        selesai();
                    }
                });
        tampilkan(binding.halaman.getCurrentItem());
    }

    private void tampilkan(int posisi) {
        boolean terakhir = posisi >= HALAMAN.length - 1;
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

    private void selesai() {
        ((MainActivity) requireActivity()).selesaiPerkenalan();
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
            h.b.foto.setImageResource(FOTO[posisi]);
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
