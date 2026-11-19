package id.lifeoffoods.notifikasi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.model.NotifikasiResponse.Notifikasi;
import id.lifeoffoods.ui.MainActivity;

/**
 * Notifikasi sistem Android untuk isi GET /notifications. Dua saluran: "War harga" dengan bunyi
 * khusus (res/raw/notif_war.wav) untuk promo, dan "Pesanan" dengan bunyi bawaan HP.
 */
public final class NotifikasiSistem {

    public static final String SALURAN_WAR = "war_harga";
    public static final String SALURAN_PESANAN = "pesanan";
    public static final String JENIS_WAR = "war_dimulai";

    private NotifikasiSistem() {}

    public static Uri bunyiWar(Context c) {
        return Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://"
                        + c.getPackageName()
                        + "/"
                        + R.raw.notif_war);
    }

    /** Aman dipanggil berulang; Android mengabaikan saluran yang sudah ada. */
    public static void buatSaluran(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel war =
                new NotificationChannel(
                        SALURAN_WAR,
                        c.getString(R.string.notif_saluran_war),
                        NotificationManager.IMPORTANCE_HIGH);
        war.setDescription(c.getString(R.string.notif_saluran_war_ket));
        war.setSound(
                bunyiWar(c),
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
        war.enableVibration(true);
        war.setVibrationPattern(new long[] {0, 120, 80, 180});
        war.enableLights(true);
        war.setLightColor(ContextCompat.getColor(c, R.color.notif_aksen));

        NotificationChannel pesanan =
                new NotificationChannel(
                        SALURAN_PESANAN,
                        c.getString(R.string.notif_saluran_pesanan),
                        NotificationManager.IMPORTANCE_HIGH);
        pesanan.setDescription(c.getString(R.string.notif_saluran_pesanan_ket));
        nm.createNotificationChannel(war);
        nm.createNotificationChannel(pesanan);
    }

    /**
     * @param foto foto jualan untuk tampilan besar; null kalau tidak ada atau gagal dimuat.
     */
    public static void tampilkan(Context c, Notifikasi n, @Nullable Bitmap foto) {
        if (!NotificationManagerCompat.from(c).areNotificationsEnabled()) {
            return;
        }
        boolean war = JENIS_WAR.equals(n.type);
        Intent buka =
                new Intent(c, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent ketuk =
                PendingIntent.getActivity(
                        c,
                        (int) n.id,
                        buka,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(c, war ? SALURAN_WAR : SALURAN_PESANAN)
                        .setSmallIcon(R.drawable.ic_lonceng)
                        .setColor(
                                ContextCompat.getColor(
                                        c, war ? R.color.notif_aksen : R.color.merek_utama))
                        .setContentTitle(n.title)
                        .setContentText(n.body)
                        .setAutoCancel(true)
                        .setContentIntent(ketuk)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(
                                war
                                        ? NotificationCompat.CATEGORY_PROMO
                                        : NotificationCompat.CATEGORY_STATUS);
        if (war) {
            // Android lama (tanpa saluran) memakai bunyi dari builder.
            b.setSound(bunyiWar(c));
            b.addAction(0, c.getString(R.string.notif_ambil_sekarang), ketuk);
        }
        if (foto != null) {
            b.setLargeIcon(foto)
                    .setStyle(
                            new NotificationCompat.BigPictureStyle()
                                    .bigPicture(foto)
                                    .bigLargeIcon((Bitmap) null)
                                    .setSummaryText(n.body));
        } else {
            b.setStyle(new NotificationCompat.BigTextStyle().bigText(n.body));
        }
        try {
            NotificationManagerCompat.from(c).notify((int) n.id, b.build());
        } catch (SecurityException e) {
            // Izin POST_NOTIFICATIONS dicabut pengguna di antara pemeriksaan dan notify.
        }
    }
}
