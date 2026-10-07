package id.lifeoffoods.ui.mitra;

import androidx.annotation.Nullable;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.data.api.model.TokoMitraDto;
import java.util.List;

/**
 * store_id toko mitra. Diambil sekali dari GET /partner/stores lalu disimpan di sesi. Selama pilot
 * satu akun mitra memegang satu toko, jadi toko pertama yang dipakai.
 */
final class TokoAktif {

    interface Hasil {
        void siap(long idToko);

        void gagal(ApiError e);
    }

    private TokoAktif() {}

    static void ambil(LofApp app, Hasil hasil) {
        long tersimpan = app.sesi().toko();
        if (tersimpan > 0) {
            hasil.siap(tersimpan);
            return;
        }
        app.api()
                .tokoSaya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<TokoMitraDto>> data) {
                                @Nullable
                                TokoMitraDto t =
                                        data == null || data.data == null || data.data.isEmpty()
                                                ? null
                                                : data.data.get(0);
                                if (t == null) {
                                    // Akun mitra baru: tokonya baru dibuat setelah
                                    // pendaftaran M03 disetujui tim.
                                    hasil.gagal(
                                            ApiError.dariRespons(
                                                    404,
                                                    "{\"message\":\"Akun ini belum punya toko.\","
                                                            + "\"code\":\""
                                                            + ApiError.BELUM_ADA_TOKO
                                                            + "\"}"));
                                    return;
                                }
                                app.sesi().simpanToko(t.id);
                                hasil.siap(t.id);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                hasil.gagal(e);
                            }
                        });
    }
}
