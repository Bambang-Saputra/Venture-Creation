package id.lifeoffoods.ui.onboarding;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.ValidasiProfil;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.ProfilBody;
import id.lifeoffoods.ui.umum.Peristiwa;

/** K04 Lengkapi profil: isi awal dari GET /me, simpan lewat PATCH /me. */
public class LengkapiProfilViewModel extends AndroidViewModel {

    public final MutableLiveData<Boolean> memuat = new MutableLiveData<>(false);

    /** Isi awal kolom, misalnya nama dan email dari akun Google. Dikirim sekali saja. */
    public final MutableLiveData<Peristiwa<MeResponse>> isiAwal = new MutableLiveData<>();

    public final MutableLiveData<String> galatNama = new MutableLiveData<>();
    public final MutableLiveData<String> galatEmail = new MutableLiveData<>();
    public final MutableLiveData<String> galatArea = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> selesai = new MutableLiveData<>();

    /** Token ditolak (401); fragment memanggil MainActivity.sesiBerakhir(). */
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private boolean sudahMuat;

    public LengkapiProfilViewModel(@NonNull Application app) {
        super(app);
    }

    /** Dipanggil tiap onViewCreated; hanya permintaan pertama yang dikirim. */
    public void muat() {
        if (sudahMuat) {
            return;
        }
        sudahMuat = true;
        app().api()
                .saya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse data) {
                                isiAwal.setValue(new Peristiwa<>(data));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // Isi awal hanya kemudahan; kolom kosong tetap bisa diisi.
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                }
                            }
                        });
    }

    public void simpan(String masukanNama, String masukanEmail, String masukanArea) {
        String nama = ValidasiProfil.nama(masukanNama);
        boolean emailSah = ValidasiProfil.emailSah(masukanEmail);
        galatNama.setValue(nama == null ? app().getString(R.string.k04_galat_nama) : null);
        galatEmail.setValue(emailSah ? null : app().getString(R.string.k04_galat_email));
        galatArea.setValue(null);
        if (nama == null || !emailSah) {
            return;
        }

        ProfilBody body = new ProfilBody();
        body.name = nama;
        body.email = ValidasiProfil.kosongJadiNull(masukanEmail);
        body.areaLabel = ValidasiProfil.kosongJadiNull(masukanArea);

        memuat.setValue(true);
        app().api()
                .ubahProfil(body)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse data) {
                                memuat.setValue(false);
                                app().sesi().profilLengkap();
                                selesai.setValue(new Peristiwa<>(true));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                memuat.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                String nama = e.pesanField("name");
                                String email = e.pesanField("email");
                                String area = e.pesanField("area_label");
                                galatNama.setValue(nama);
                                galatEmail.setValue(email);
                                galatArea.setValue(area);
                                if (nama == null && email == null && area == null) {
                                    galat.setValue(new Peristiwa<>(e.pesan()));
                                }
                            }
                        });
    }

    private LofApp app() {
        return getApplication();
    }
}
