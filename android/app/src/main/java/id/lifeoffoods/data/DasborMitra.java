package id.lifeoffoods.data;

import androidx.annotation.Nullable;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.LaporanMingguanDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Aturan M05 Dashboard mitra (PRD-24), Java murni supaya bisa diuji unit. */
public final class DasborMitra {

    /** Tas yang tampil di kartu "Tas aktif hari ini"; sisanya lewat "Kelola" (M10). */
    public static final int TAS_MAKS = 3;

    private DasborMitra() {}

    /** Sisa waktu sampai tutup untuk pengingat: jam penuh kalau 60 menit atau lebih. */
    public static final class SisaWaktu {
        public final boolean jam;
        public final int angka;

        SisaWaktu(boolean jam, int angka) {
            this.jam = jam;
            this.angka = angka;
        }
    }

    /** null kalau toko sudah tutup atau jam tutup tidak diketahui. */
    @Nullable
    public static SisaWaktu sisaWaktu(@Nullable Integer menit) {
        if (menit == null || menit <= 0) {
            return null;
        }
        return menit >= 60 ? new SisaWaktu(true, menit / 60) : new SisaWaktu(false, menit);
    }

    /** Ada minimal satu hari yang dicatat pekan ini. */
    public static boolean adaCatatan(@Nullable List<LaporanMingguanDto.Harian> daily) {
        if (daily == null) {
            return false;
        }
        for (LaporanMingguanDto.Harian h : daily) {
            if (h.wastedValueRupiah != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tas hari ini untuk dashboard: yang masih bisa dibeli dulu, lalu dijeda atau draf, lalu habis,
     * terakhir yang lewat jam ambil. Urutan asli dipertahankan di dalam kelompok yang sama.
     */
    public static List<JualanMitraDto> tasHariIni(@Nullable List<JualanMitraDto> jualan) {
        List<JualanMitraDto> d = new ArrayList<>();
        if (jualan == null) {
            return d;
        }
        for (JualanMitraDto j : jualan) {
            if (j != null && !JualanMitraDto.TIPE_MENU.equals(j.type)) {
                d.add(j);
            }
        }
        d.sort(Comparator.comparingInt(j -> urutan(j.status)));
        return d.size() > TAS_MAKS ? new ArrayList<>(d.subList(0, TAS_MAKS)) : d;
    }

    private static int urutan(@Nullable String status) {
        if (JualanMitraDto.AKTIF.equals(status)) {
            return 0;
        }
        if (JualanMitraDto.DIJEDA.equals(status) || JualanMitraDto.DRAF.equals(status)) {
            return 1;
        }
        if (JualanMitraDto.HABIS.equals(status)) {
            return 2;
        }
        return 3;
    }
}
