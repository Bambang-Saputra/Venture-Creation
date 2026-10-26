package id.lifeoffoods.ui.voucher;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.DaftarVoucherDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.data.api.model.VoucherDto;
import id.lifeoffoods.ui.umum.Peristiwa;

/** K22 Voucher saya: GET /vouchers dan klaim voucher pop up. */
public class VoucherViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<DaftarVoucherDto> daftar = new MutableLiveData<>();
    public final MutableLiveData<Boolean> mengklaim = new MutableLiveData<>(false);
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<VoucherDto>> diklaim = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    public VoucherViewModel(@NonNull Application app) {
        super(app);
    }

    public void muatUlang() {
        status.setValue(Status.MEMUAT);
        app().api()
                .voucher()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<DaftarVoucherDto> data) {
                                daftar.setValue(data == null ? null : data.data);
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                }
                                galat.setValue(new Peristiwa<>(e.pesan()));
                                status.setValue(Status.GAGAL);
                            }
                        });
    }

    public void klaim(long voucherId) {
        if (Boolean.TRUE.equals(mengklaim.getValue())) {
            return;
        }
        mengklaim.setValue(true);
        app().api()
                .klaimVoucher(voucherId)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<VoucherDto> data) {
                                mengklaim.setValue(false);
                                if (data != null && data.data != null) {
                                    diklaim.setValue(new Peristiwa<>(data.data));
                                }
                                muatUlang();
                            }

                            @Override
                            public void gagal(ApiError e) {
                                mengklaim.setValue(false);
                                galat.setValue(new Peristiwa<>(e.pesan()));
                                muatUlang();
                            }
                        });
    }

    private LofApp app() {
        return getApplication();
    }
}
