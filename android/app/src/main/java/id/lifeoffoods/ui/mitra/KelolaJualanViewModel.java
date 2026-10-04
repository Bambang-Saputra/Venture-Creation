package id.lifeoffoods.ui.mitra;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import id.lifeoffoods.LofApp;
import id.lifeoffoods.data.api.ApiCallback;
import id.lifeoffoods.data.api.ApiError;
import id.lifeoffoods.data.api.model.AkunDto;
import id.lifeoffoods.data.api.model.AlergenDto;
import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.Terbungkus;
import id.lifeoffoods.ui.umum.FotoUnggah;
import id.lifeoffoods.ui.umum.Peristiwa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * M10 Kelola jualan (tas kejutan) dan M17 Kelola menu satuan (PRD-09): satu layar dengan segmen.
 * Saklar tiap kartu memanggil publish atau pause; hasilnya menggantikan kartu itu saja.
 */
public class KelolaJualanViewModel extends AndroidViewModel {

    public enum Status {
        MEMUAT,
        SIAP,
        GAGAL
    }

    public final MutableLiveData<String> tipe = new MutableLiveData<>(JualanMitraDto.TIPE_TAS);
    public final MutableLiveData<Status> status = new MutableLiveData<>(Status.MEMUAT);
    public final MutableLiveData<List<JualanMitraDto>> daftar =
            new MutableLiveData<>(Collections.emptyList());

    /** Kode alergen ke nama, dari GET /allergens (respons jualan hanya berisi kode). */
    public final MutableLiveData<Map<String, String>> namaAlergen =
            new MutableLiveData<>(Collections.emptyMap());

    /** Id jualan yang saklarnya sedang diproses. */
    public final MutableLiveData<Set<Long>> sibuk = new MutableLiveData<>(Collections.emptySet());

    public final MutableLiveData<String> galatAwal = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<String>> galat = new MutableLiveData<>();
    public final MutableLiveData<Peristiwa<Boolean>> sesiBerakhir = new MutableLiveData<>();

    private int generasi;
    private boolean sudahAlergen;

    public KelolaJualanViewModel(@NonNull Application app) {
        super(app);
    }

    /** Dari argumen (M17 membuka segmen menu satuan). Hanya berlaku sekali. */
    public void mulai(String tipeAwal) {
        if (daftar.getValue() == null || daftar.getValue().isEmpty()) {
            tipe.setValue(tipeAwal);
        }
    }

    public void pilihTipe(String t) {
        if (t.equals(tipe.getValue())) {
            return;
        }
        tipe.setValue(t);
        daftar.setValue(Collections.emptyList());
        status.setValue(Status.MEMUAT);
        muat();
    }

    public void muat() {
        int gen = ++generasi;
        LofApp app = getApplication();
        muatAlergen(app);
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        if (gen != generasi) {
                            return;
                        }
                        app.api()
                                .jualanMitra(idToko, tipe.getValue())
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(
                                                    Terbungkus<List<JualanMitraDto>> data) {
                                                if (gen != generasi) {
                                                    return;
                                                }
                                                daftar.setValue(
                                                        data == null || data.data == null
                                                                ? Collections.emptyList()
                                                                : data.data);
                                                status.setValue(Status.SIAP);
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                if (gen == generasi) {
                                                    gagalMuat(e);
                                                }
                                            }
                                        });
                    }

                    @Override
                    public void gagal(ApiError e) {
                        if (gen == generasi) {
                            gagalMuat(e);
                        }
                    }
                });
    }

    private void muatAlergen(LofApp app) {
        if (sudahAlergen) {
            return;
        }
        sudahAlergen = true;
        app.api()
                .daftarAlergen()
                .enqueue(
                        new ApiCallback<>() {
                            @Override
                            public void sukses(Terbungkus<List<AlergenDto>> data) {
                                Map<String, String> m = new HashMap<>();
                                if (data != null && data.data != null) {
                                    for (AlergenDto a : data.data) {
                                        m.put(a.code, a.name);
                                    }
                                }
                                namaAlergen.setValue(m);
                            }

                            @Override
                            public void gagal(ApiError e) {
                                // Tanpa nama, chip menampilkan kodenya. Coba lagi saat dimuat
                                // ulang.
                                sudahAlergen = false;
                            }
                        });
    }

    private void gagalMuat(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        if (status.getValue() == Status.SIAP) {
            galat.setValue(new Peristiwa<>(e.pesan()));
        } else {
            // 403 untuk kasir: pesan sopan dari server ("hanya pemilik ...") tampil di sini.
            galatAwal.setValue(e.pesan());
            status.setValue(Status.GAGAL);
        }
    }

    /** Saklar kartu: nyala = terbitkan, mati = jeda. */
    public void ubahSaklar(JualanMitraDto j, boolean nyala) {
        Set<Long> s =
                new HashSet<>(sibuk.getValue() == null ? Collections.emptySet() : sibuk.getValue());
        if (!s.add(j.id)) {
            return;
        }
        sibuk.setValue(s);
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        (nyala
                                        ? app.api().terbitkanJualan(idToko, j.id)
                                        : app.api().jedaJualan(idToko, j.id))
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(Terbungkus<JualanMitraDto> data) {
                                                selesai(j.id);
                                                if (data != null && data.data != null) {
                                                    ganti(data.data);
                                                }
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                selesai(j.id);
                                                gagalSaklar(e);
                                            }
                                        });
                    }

                    @Override
                    public void gagal(ApiError e) {
                        selesai(j.id);
                        gagalSaklar(e);
                    }
                });
    }

    /** Ketuk slot foto di kartu M10: unggah foto untuk jualan itu saja. */
    public void unggahFoto(JualanMitraDto j, byte[] jpeg) {
        Set<Long> s =
                new HashSet<>(sibuk.getValue() == null ? Collections.emptySet() : sibuk.getValue());
        if (!s.add(j.id)) {
            return;
        }
        sibuk.setValue(s);
        LofApp app = getApplication();
        TokoAktif.ambil(
                app,
                new TokoAktif.Hasil() {
                    @Override
                    public void siap(long idToko) {
                        app.api()
                                .unggahFotoJualan(
                                        idToko, j.id, FotoUnggah.bagian(jpeg, "jualan.jpg"))
                                .enqueue(
                                        new ApiCallback<>() {
                                            @Override
                                            public void sukses(AkunDto.Foto data) {
                                                selesai(j.id);
                                                if (data != null) {
                                                    j.photoUrl = data.photoUrl;
                                                    ganti(j);
                                                }
                                            }

                                            @Override
                                            public void gagal(ApiError e) {
                                                selesai(j.id);
                                                gagalFoto(e);
                                            }
                                        });
                    }

                    @Override
                    public void gagal(ApiError e) {
                        selesai(j.id);
                        gagalFoto(e);
                    }
                });
    }

    private void gagalFoto(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        String g = e.pesanField("photo");
        galat.setValue(new Peristiwa<>(g != null ? g : e.pesan()));
    }

    private void gagalSaklar(ApiError e) {
        if (e.perluMasukUlang()) {
            sesiBerakhir.setValue(new Peristiwa<>(true));
            return;
        }
        galat.setValue(new Peristiwa<>(e.pesan()));
        // Status di server mungkin sudah berubah (misalnya habis): muat ulang supaya saklar benar.
        muat();
    }

    private void selesai(long id) {
        Set<Long> s =
                new HashSet<>(sibuk.getValue() == null ? Collections.emptySet() : sibuk.getValue());
        s.remove(id);
        sibuk.setValue(s);
    }

    private void ganti(JualanMitraDto baru) {
        List<JualanMitraDto> d =
                new ArrayList<>(
                        daftar.getValue() == null ? Collections.emptyList() : daftar.getValue());
        for (int i = 0; i < d.size(); i++) {
            if (d.get(i).id == baru.id) {
                d.set(i, baru);
            }
        }
        daftar.setValue(d);
    }
}
