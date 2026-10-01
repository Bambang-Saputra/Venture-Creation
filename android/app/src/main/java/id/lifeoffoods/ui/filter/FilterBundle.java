package id.lifeoffoods.ui.filter;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import id.lifeoffoods.data.FilterJualan;
import java.util.ArrayList;

/**
 * Mengemas {@link FilterJualan} ke Bundle untuk argumen K09 dan hasil K09 ke K07. Dipisah dari
 * FilterJualan supaya kelas itu tetap Java murni dan bisa diuji tanpa Android.
 */
public final class FilterBundle {

    /** Kunci setFragmentResult dari K09 ke K07. */
    public static final String HASIL = "hasil_filter_jualan";

    /** Kunci argumen dan isi hasil. */
    public static final String ARG = "filter_jualan";

    private static final String TAS = "tas";
    private static final String MENU = "menu";
    private static final String KODE = "alergen_kode";
    private static final String NAMA = "alergen_nama";
    private static final String SEMBUNYIKAN = "sembunyikan_alergi";
    private static final String HALAL = "halal";
    private static final String RADIUS = "radius_km";
    private static final String JAM = "jam";

    private FilterBundle() {}

    @NonNull
    public static Bundle ke(@NonNull FilterJualan f) {
        Bundle b = new Bundle();
        b.putBoolean(TAS, f.tas);
        b.putBoolean(MENU, f.menu);
        b.putStringArrayList(KODE, new ArrayList<>(f.alergen.keySet()));
        b.putStringArrayList(NAMA, new ArrayList<>(f.alergen.values()));
        b.putBoolean(SEMBUNYIKAN, f.sembunyikanAlergi);
        b.putBoolean(HALAL, f.halal);
        b.putInt(RADIUS, f.radiusKm == null ? 0 : f.radiusKm);
        b.putString(JAM, f.jam);
        Bundle luar = new Bundle();
        luar.putBundle(ARG, b);
        return luar;
    }

    /** null kalau Bundle tidak membawa filter (misalnya K09 dibuka tanpa argumen). */
    @Nullable
    public static FilterJualan dari(@Nullable Bundle luar) {
        Bundle b = luar == null ? null : luar.getBundle(ARG);
        if (b == null) {
            return null;
        }
        FilterJualan f = new FilterJualan();
        f.tas = b.getBoolean(TAS, true);
        f.menu = b.getBoolean(MENU, true);
        ArrayList<String> kode = b.getStringArrayList(KODE);
        ArrayList<String> nama = b.getStringArrayList(NAMA);
        if (kode != null) {
            for (int i = 0; i < kode.size(); i++) {
                f.alergen.put(kode.get(i), nama != null && i < nama.size() ? nama.get(i) : "");
            }
        }
        f.sembunyikanAlergi = b.getBoolean(SEMBUNYIKAN, true);
        f.halal = b.getBoolean(HALAL, false);
        int radius = b.getInt(RADIUS, 0);
        f.radiusKm = radius > 0 ? radius : null;
        f.jam = b.getString(JAM);
        return f;
    }
}
