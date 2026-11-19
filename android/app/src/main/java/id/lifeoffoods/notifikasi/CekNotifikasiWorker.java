package id.lifeoffoods.notifikasi;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.bumptech.glide.Glide;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.data.api.model.NotifikasiResponse.Notifikasi;
import java.util.concurrent.TimeUnit;
import retrofit2.Response;

/**
 * Pekerja latar tanpa Firebase: kira-kira tiap 15 menit (batas minimum WorkManager) membaca GET
 * /notifications?unread=1 dan menampilkan yang baru sebagai notifikasi sistem berbunyi.
 */
public class CekNotifikasiWorker extends Worker {

    private static final String NAMA = "cek_notifikasi";

    public CekNotifikasiWorker(@NonNull Context c, @NonNull WorkerParameters p) {
        super(c, p);
    }

    public static void jadwalkan(Context c) {
        PeriodicWorkRequest kerja =
                new PeriodicWorkRequest.Builder(CekNotifikasiWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(
                                new Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build())
                        .build();
        WorkManager.getInstance(c)
                .enqueueUniquePeriodicWork(NAMA, ExistingPeriodicWorkPolicy.KEEP, kerja);
    }

    @NonNull
    @Override
    public Result doWork() {
        LofApp app = (LofApp) getApplicationContext();
        if (!app.sesi().sudahMasuk()) {
            return Result.success();
        }
        try {
            Response<NotifikasiResponse> r = app.api().notifikasi(1).execute();
            if (!r.isSuccessful()) {
                // 401 dan sejenisnya ditangani saat aplikasi dibuka; jangan diulang terus.
                return Result.success();
            }
            for (Notifikasi n : CatatanNotifikasi.ambilBaru(app, r.body())) {
                NotifikasiSistem.tampilkan(app, n, foto(app, n));
            }
            return Result.success();
        } catch (java.io.IOException e) {
            return Result.retry();
        }
    }

    /** Foto jualan untuk BigPictureStyle; null kalau tidak ada atau gagal dimuat. */
    @Nullable
    static Bitmap foto(Context c, Notifikasi n) {
        Object url = n.data == null ? null : n.data.get("photo_url");
        if (!(url instanceof String) || ((String) url).isEmpty()) {
            return null;
        }
        try {
            return Glide.with(c).asBitmap().load((String) url).centerCrop().submit(720, 360).get();
        } catch (Exception e) {
            return null;
        }
    }
}
