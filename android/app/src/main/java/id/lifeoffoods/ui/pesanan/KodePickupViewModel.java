package id.lifeoffoods.ui.pesanan;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.PesananDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;

/** K14: GET /orders/{id}. Dibuka sesudah pesanan dibuat, dan nanti dari K15 atau notifikasi. */
public class KodePickupViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<PesananDto> pesanan = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private long id;
    private boolean sudahMuat;

    public KodePickupViewModel(@NonNull Application app) {
        super(app);
    }

    public void muat(long id) {
        if (sudahMuat && this.id == id) {
            return;
        }
        sudahMuat = true;
        this.id = id;
        muatUlang();
    }

    public void muatUlang() {
        status.setValue(Status.MEMUAT);
        ((LofApp) getApplication())
                .api()
                .detailPesanan(id)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<PesananDto> data) {
                                if (data == null || data.data == null) {
                                    status.setValue(Status.GAGAL);
                                    return;
                                }
                                pesanan.setValue(data.data);
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                status.setValue(Status.GAGAL);
                            }
                        });
    }
}
