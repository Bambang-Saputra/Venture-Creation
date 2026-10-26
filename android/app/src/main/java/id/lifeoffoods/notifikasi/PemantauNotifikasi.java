package id.lifeoffoods.notifikasi;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.SesiPengguna;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.data.api.model.NotifikasiResponse.Notifikasi;
import id.lifeoffoods.databinding.PopupNotifikasiBinding;
import id.lifeoffoods.ui.MainActivity;

/**
 * Selama aplikasi terbuka, periksa notifikasi baru tiap menit. Yang baru muncul sebagai pop up di
 * atas layar dengan bunyi "war", lalu hilang sendiri. Ketuk pop up untuk membuka K17 (pembeli) atau
 * M11 (mitra). Juga meminta izin notifikasi sekali, sesudah pengguna masuk.
 */
public final class PemantauNotifikasi implements Application.ActivityLifecycleCallbacks {

    private static final long JEDA_MS = 60_000;
    private static final long TAMPIL_MS = 5_000;

    private final LofApp app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Nullable private Activity aktif;

    private final Runnable periksa =
            new Runnable() {
                @Override
                public void run() {
                    ambil();
                    handler.postDelayed(this, JEDA_MS);
                }
            };

    public PemantauNotifikasi(LofApp app) {
        this.app = app;
    }

    @Override
    public void onActivityResumed(@NonNull Activity a) {
        if (!(a instanceof MainActivity)) {
            return;
        }
        aktif = a;
        mintaIzin(a);
        handler.removeCallbacks(periksa);
        handler.post(periksa);
    }

    @Override
    public void onActivityPaused(@NonNull Activity a) {
        if (a == aktif) {
            handler.removeCallbacks(periksa);
            aktif = null;
        }
    }

    private void ambil() {
        if (!app.sesi().sudahMasuk()) {
            return;
        }
        app.api()
                .notifikasi(1)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(NotifikasiResponse data) {
                                for (Notifikasi n : CatatanNotifikasi.ambilBaru(app, data)) {
                                    if (aktif != null) {
                                        tampilkanPopup(aktif, n);
                                    }
                                }
                            }

                            @Override
                            public void gagal(ApiError galat) {
                                // Diam saja; layar yang terbuka menangani sesi berakhir sendiri.
                            }
                        });
    }

    private void tampilkanPopup(Activity a, Notifikasi n) {
        ViewGroup akar = a.findViewById(android.R.id.content);
        if (akar == null) {
            return;
        }
        PopupNotifikasiBinding b =
                PopupNotifikasiBinding.inflate(LayoutInflater.from(a), akar, false);
        boolean war = NotifikasiSistem.JENIS_WAR.equals(n.type);
        b.judul.setText(n.title);
        b.isi.setText(n.body);
        b.isi.setVisibility(n.body == null ? View.GONE : View.VISIBLE);
        if (war) {
            b.ikon.setImageResource(R.drawable.ic_api);
            b.ikon.setColorFilter(ContextCompat.getColor(a, R.color.notif_aksen));
        }
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP);
        lp.topMargin = Math.round(40 * a.getResources().getDisplayMetrics().density);
        View v = b.getRoot();
        akar.addView(v, lp);
        v.setTranslationY(-300f);
        v.setAlpha(0f);
        v.animate().translationY(0f).alpha(1f).setDuration(250).start();
        Runnable tutup =
                () ->
                        v.animate()
                                .translationY(-300f)
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction(() -> akar.removeView(v))
                                .start();
        v.setOnClickListener(
                x -> {
                    tutup.run();
                    bukaDaftar(a);
                });
        handler.postDelayed(tutup, TAMPIL_MS);
        bunyikan(a, war);
    }

    private void bukaDaftar(Activity a) {
        boolean mitra = SesiPengguna.PERAN_MITRA.equals(app.sesi().peran());
        try {
            Navigation.findNavController(a, R.id.nav_host)
                    .navigate(mitra ? R.id.m11_pesanan : R.id.k17_notifikasi);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Graf yang aktif (misalnya alur masuk) tidak punya tujuan itu; biarkan.
        }
    }

    private static void bunyikan(Activity a, boolean war) {
        try {
            Ringtone r =
                    RingtoneManager.getRingtone(
                            a,
                            war
                                    ? NotifikasiSistem.bunyiWar(a)
                                    : RingtoneManager.getDefaultUri(
                                            RingtoneManager.TYPE_NOTIFICATION));
            if (r != null) {
                r.play();
            }
        } catch (RuntimeException e) {
            // Mode senyap atau pemutar tidak tersedia: pop up tetap tampil tanpa bunyi.
        }
    }

    /** Android 13+: izin notifikasi diminta sekali, sesudah pengguna masuk. */
    private void mintaIzin(Activity a) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || !app.sesi().sudahMasuk()
                || CatatanNotifikasi.izinSudahDitanya(a)
                || ContextCompat.checkSelfPermission(a, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        CatatanNotifikasi.tandaiIzinDitanya(a);
        ActivityCompat.requestPermissions(
                a, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 7101);
    }

    @Override
    public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) {}

    @Override
    public void onActivityStarted(@NonNull Activity a) {}

    @Override
    public void onActivityStopped(@NonNull Activity a) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity a) {}
}
