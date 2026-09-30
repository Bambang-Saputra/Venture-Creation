package id.lifeoffoods.ui.mitra;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import id.lifeoffoods.R;
import id.lifeoffoods.data.JualanMitra;
import id.lifeoffoods.databinding.IncludeLabelTokoBinding;
import id.lifeoffoods.databinding.IncludePilihJamBinding;
import java.util.Locale;
import java.util.Map;

/** Bagian tampilan bersama M09 dan M16: jam ambil, saklar label, dan teks galat. */
final class TampilanPasang {

    private TampilanPasang() {}

    static void pasangJam(Fragment f, IncludePilihJamBinding b, PasangJualanViewModel vm) {
        b.tombolMulai.setOnClickListener(v -> bukaJam(f, vm, true));
        b.tombolAkhir.setOnClickListener(v -> bukaJam(f, vm, false));
        vm.mulai.observe(
                f.getViewLifecycleOwner(),
                m -> isiJam(f, b.tombolMulai, m, R.string.m09_jam_mulai));
        vm.akhir.observe(
                f.getViewLifecycleOwner(),
                m -> isiJam(f, b.tombolAkhir, m, R.string.m09_jam_akhir));
    }

    private static void isiJam(Fragment f, TextView t, @Nullable Integer m, @StringRes int label) {
        String jam =
                m == null || m < 0
                        ? "--.--"
                        : String.format(Locale.ROOT, "%02d.%02d", m / 60, m % 60);
        t.setText(jam);
        t.setContentDescription(f.getString(label) + " " + jam);
    }

    private static void bukaJam(Fragment f, PasangJualanViewModel vm, boolean awal) {
        Integer sekarang = (awal ? vm.mulai : vm.akhir).getValue();
        int m = sekarang == null || sekarang < 0 ? (awal ? 19 * 60 : 21 * 60) : sekarang;
        MaterialTimePicker p =
                new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(m / 60)
                        .setMinute(m % 60)
                        .setTitleText(awal ? R.string.m09_jam_mulai : R.string.m09_jam_akhir)
                        .build();
        p.addOnPositiveButtonClickListener(v -> vm.ubahJam(awal, p.getHour() * 60 + p.getMinute()));
        p.show(f.getChildFragmentManager(), "jam");
    }

    static void pasangLabel(Fragment f, IncludeLabelTokoBinding b, PasangJualanViewModel vm) {
        vm.halal.observe(
                f.getViewLifecycleOwner(),
                n -> {
                    if (b.saklarHalal.isChecked() != Boolean.TRUE.equals(n)) {
                        b.saklarHalal.setChecked(Boolean.TRUE.equals(n));
                    }
                });
        vm.dapurKacang.observe(
                f.getViewLifecycleOwner(),
                n -> {
                    if (b.saklarKacang.isChecked() != Boolean.TRUE.equals(n)) {
                        b.saklarKacang.setChecked(Boolean.TRUE.equals(n));
                    }
                });
        b.saklarHalal.setOnCheckedChangeListener((s, n) -> vm.ubahHalal(n));
        b.saklarKacang.setOnCheckedChangeListener((s, n) -> vm.ubahDapurKacang(n));
    }

    /** Teks galat lokal (kode JualanMitra.G_*). */
    @StringRes
    static int teks(int kode, boolean menu) {
        switch (kode) {
            case JualanMitra.G_JUDUL:
                return R.string.m09_g_judul;
            case JualanMitra.G_HARGA:
                return R.string.m09_g_harga;
            case JualanMitra.G_HARGA_NORMAL:
                return R.string.m09_g_harga_normal;
            case JualanMitra.G_KANDUNGAN:
                return R.string.m09_g_kandungan;
            case JualanMitra.G_JAM:
                return R.string.m09_g_jam;
            case JualanMitra.G_ALERGEN:
                return menu ? R.string.m16_g_alergen : R.string.m09_g_alergen;
            case JualanMitra.G_ITEM:
                return R.string.m16_g_item;
            default:
                return R.string.m09_g_jumlah;
        }
    }

    /** Pesan untuk field, dari galat server (sudah berupa teks) atau null. */
    static void tampilGalat(TextView t, @Nullable Map<String, String> galat, String... field) {
        String pesan = null;
        if (galat != null) {
            for (String f : field) {
                if (galat.containsKey(f)) {
                    pesan = galat.get(f);
                    break;
                }
            }
        }
        t.setText(pesan);
        t.setVisibility(pesan == null ? View.GONE : View.VISIBLE);
    }
}
