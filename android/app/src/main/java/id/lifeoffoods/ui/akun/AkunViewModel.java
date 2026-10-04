package id.lifeoffoods.ui.akun;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.R;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.AkunDto;
import id.lifeoffoods.data.api.model.MeResponse;
import id.lifeoffoods.data.api.model.ProfilBody;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.Peristiwa;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Akun pembeli untuk K18 Profil, K19 Edit profil, dan K20 Pengaturan. Tiap layar memegang instance
 * sendiri dan memuat ulang GET /me saat tampil, jadi perubahan di satu layar terlihat di layar
 * lain.
 */
public class AkunViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<MeResponse> saya = new MutableLiveData<>();
    public final MutableLiveData<AkunDto.Dampak> dampak = new MutableLiveData<>();
    public final MutableLiveData<String> galat = new MutableLiveData<>();
    public final MutableLiveData<Boolean> menyimpan = new MutableLiveData<>(false);
    public final MutableLiveData<Peristiwa<String>> pesan = new MutableLiveData<>();

    /** Galat 422 per field dari PATCH /me (K19). */
    public final MutableLiveData<ApiError> galatIsian = new MutableLiveData<>();

    public final MutableLiveData<Peristiwa<Boolean>> tersimpan = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> akunTerhapus = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    public AkunViewModel(@NonNull Application app) {
        super(app);
    }

    /** Dari onResume. {@code denganDampak} untuk header K18. */
    public void segarkan(boolean denganDampak) {
        if (saya.getValue() == null) {
            status.setValue(Status.MEMUAT);
        }
        LofApp app = getApplication();
        app.api()
                .saya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse data) {
                                if (data == null || data.user == null) {
                                    gagalMuat(ApiError.jaringan());
                                    return;
                                }
                                saya.setValue(data);
                                status.setValue(Status.SIAP);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                gagalMuat(e);
                            }
                        });
        if (!denganDampak) {
            return;
        }
        app.api()
                .dampakSaya()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<AkunDto.Dampak> data) {
                                if (data != null && data.data != null) {
                                    dampak.setValue(data.data);
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // Ubin dampak hanya pelengkap; layar tetap jalan tanpa angka.
                            }
                        });
    }

    /** K19 Simpan perubahan. Email kosong dikirim "" supaya server mengosongkannya. */
    public void simpanProfil(String nama, String email, String area) {
        ProfilBody body = new ProfilBody();
        body.name = nama;
        body.email = email;
        body.areaLabel = area.isEmpty() ? null : area;
        kirim(body, true);
    }

    /** K20 sakelar notifikasi: kirim satu field yang berubah saja. */
    public void ubahSakelar(@NonNull ProfilBody body) {
        kirim(body, false);
    }

    private void kirim(ProfilBody body, boolean dariFormulir) {
        if (Boolean.TRUE.equals(menyimpan.getValue())) {
            return;
        }
        menyimpan.setValue(true);
        galatIsian.setValue(null);
        LofApp app = getApplication();
        app.api()
                .ubahProfil(body)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(MeResponse data) {
                                menyimpan.setValue(false);
                                if (data != null && data.user != null) {
                                    saya.setValue(data);
                                }
                                if (dariFormulir) {
                                    tersimpan.setValue(new Peristiwa<>(true));
                                }
                            }

                            @Override
                            public void gagal(ApiError e) {
                                menyimpan.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                if (dariFormulir && e.kode() == 422) {
                                    galatIsian.setValue(e);
                                    return;
                                }
                                // Sakelar kembali ke nilai tersimpan.
                                saya.setValue(saya.getValue());
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    /** K19 Ganti foto. {@code jpeg} sudah diperkecil oleh {@link FotoProfil}. */
    public void unggahFoto(byte[] jpeg) {
        menyimpan.setValue(true);
        RequestBody isi = RequestBody.create(jpeg, MediaType.get("image/jpeg"));
        MultipartBody.Part bagian = MultipartBody.Part.createFormData("photo", "profil.jpg", isi);
        LofApp app = getApplication();
        app.api()
                .unggahFotoProfil(bagian)
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(AkunDto.Foto data) {
                                menyimpan.setValue(false);
                                MeResponse m = saya.getValue();
                                if (m != null && m.user != null && data != null) {
                                    m.user.photoUrl = data.photoUrl;
                                    saya.setValue(m);
                                }
                                pesan.setValue(
                                        new Peristiwa<>(
                                                app.getString(R.string.k19_foto_tersimpan)));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                menyimpan.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                String field = e.pesanField("photo");
                                pesan.setValue(new Peristiwa<>(field != null ? field : e.pesan()));
                            }
                        });
    }

    /** K20 Hapus akun. Berhasil: sesi lokal dihapus, layar kembali ke K01. */
    public void hapusAkun() {
        if (Boolean.TRUE.equals(menyimpan.getValue())) {
            return;
        }
        menyimpan.setValue(true);
        LofApp app = getApplication();
        app.api()
                .hapusAkun(new AkunDto.HapusAkun())
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Void data) {
                                menyimpan.setValue(false);
                                app.sesi().hapus();
                                akunTerhapus.setValue(new Peristiwa<>(true));
                            }

                            @Override
                            public void gagal(ApiError e) {
                                menyimpan.setValue(false);
                                if (e.perluMasukUlang()) {
                                    sesiBerakhir.setValue(new Peristiwa<>(true));
                                    return;
                                }
                                // 409: masih ada pesanan yang belum diambil.
                                pesan.setValue(new Peristiwa<>(e.pesan()));
                            }
                        });
    }

    @Nullable
    public MeResponse.ConsumerProfile profil() {
        MeResponse m = saya.getValue();
        return m == null ? null : m.consumerProfile;
    }

    private void gagalMuat(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (saya.getValue() != null) {
            return;
        }
        galat.setValue(e.pesan());
        status.setValue(Status.GAGAL);
    }
}
