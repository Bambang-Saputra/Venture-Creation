package id.lifeoffoods.ui.mitra;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import id.lifeoffoods.R;
import id.lifeoffoods.data.FormatTampilan;
import id.lifeoffoods.data.LaporanMingguan;
import id.lifeoffoods.data.api.model.LaporanMingguanDto;
import id.lifeoffoods.databinding.ItemBatangHarianBinding;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Grafik batang nilai sisa Senin sampai Minggu, dipakai M05 Dashboard dan M07 Laporan mingguan.
 * Hari tertinggi berwarna merek; hari yang tidak dicatat tampil sebagai kotak bergaris putus, bukan
 * 0.
 */
final class GrafikHarian {

    private static final Locale ID = new Locale("id", "ID");
    private static final DateTimeFormatter HARI_PANJANG = DateTimeFormatter.ofPattern("EEEE", ID);

    private static final int BATANG_MIN_DP = 4;
    private static final int GARIS_NOL_DP = 2;
    private static final int CELAH_DP = 24;

    private GrafikHarian() {}

    /**
     * @param penuhDp tinggi batang tertinggi (Figma M07: 100, M05: 124)
     * @param angka tampilkan angka ribu rupiah di atas batang (M07 ya, M05 tidak)
     * @return true kalau ada hari yang tidak dicatat
     */
    static boolean isi(
            LinearLayout grafik,
            LocalDate senin,
            @Nullable List<LaporanMingguanDto.Harian> daily,
            int penuhDp,
            boolean angka) {
        grafik.removeAllViews();
        Context ctx = grafik.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        List<Long> nilai = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            nilai.add(nilaiHari(daily, senin.plusDays(i)));
        }
        long maks = LaporanMingguan.maks(nilai);
        float dp = ctx.getResources().getDisplayMetrics().density;
        boolean adaCelah = false;
        for (int i = 0; i < 7; i++) {
            LocalDate hari = senin.plusDays(i);
            Long n = nilai.get(i);
            boolean teratas = n != null && maks > 0 && n == maks;
            ItemBatangHarianBinding b = ItemBatangHarianBinding.inflate(inflater, grafik, false);
            b.hari.setText(hari.getDayOfWeek().getDisplayName(TextStyle.SHORT, ID));
            b.angka.setVisibility(angka ? View.VISIBLE : View.GONE);
            String panjang = kapital(hari.format(HARI_PANJANG));
            ViewGroup.LayoutParams lp = b.batang.getLayoutParams();
            if (n == null) {
                adaCelah = true;
                b.angka.setText("–");
                b.batang.setBackgroundResource(R.drawable.bg_batang_kosong);
                lp.height = Math.round(CELAH_DP * dp);
                b.getRoot().setContentDescription(ctx.getString(R.string.m07_hari_kosong, panjang));
            } else {
                int t =
                        LaporanMingguan.tinggi(
                                n, maks, Math.round(penuhDp * dp), Math.round(BATANG_MIN_DP * dp));
                lp.height = t > 0 ? t : Math.round(GARIS_NOL_DP * dp);
                b.angka.setText(LaporanMingguan.ribu(n));
                b.batang.setBackgroundResource(
                        teratas
                                ? R.drawable.bg_batang_harian_aktif
                                : R.drawable.bg_batang_harian_pasif);
                if (teratas) {
                    b.angka.setTextColor(ContextCompat.getColor(ctx, R.color.teks_merek));
                }
                b.getRoot()
                        .setContentDescription(
                                ctx.getString(
                                        teratas
                                                ? R.string.m07_hari_teratas
                                                : R.string.m07_hari_nilai,
                                        panjang,
                                        FormatTampilan.rupiah(n)));
            }
            b.batang.setLayoutParams(lp);
            grafik.addView(b.getRoot());
        }
        return adaCelah;
    }

    @Nullable
    private static Long nilaiHari(@Nullable List<LaporanMingguanDto.Harian> daily, LocalDate hari) {
        if (daily == null) {
            return null;
        }
        for (LaporanMingguanDto.Harian h : daily) {
            try {
                if (h.date != null && LocalDate.parse(h.date).equals(hari)) {
                    return h.wastedValueRupiah;
                }
            } catch (DateTimeParseException e) {
                // Tanggal rusak dilewati; hari itu tampil sebagai tidak dicatat.
            }
        }
        return null;
    }

    static String kapital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
