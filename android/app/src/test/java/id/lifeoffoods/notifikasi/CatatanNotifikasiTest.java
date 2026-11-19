package id.lifeoffoods.notifikasi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import id.lifeoffoods.data.api.model.NotifikasiResponse;
import id.lifeoffoods.data.api.model.NotifikasiResponse.Notifikasi;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CatatanNotifikasiTest {

    @Test
    public void pemeriksaanPertamaHanyaTitikAwalLaluHanyaYangBaru() {
        Context c = RuntimeEnvironment.getApplication();
        CatatanNotifikasi.lupakan(c);

        assertTrue(
                "notifikasi lama tidak dibanjirkan",
                CatatanNotifikasi.ambilBaru(c, respons(n(5, false), n(4, false))).isEmpty());

        List<Notifikasi> baru =
                CatatanNotifikasi.ambilBaru(c, respons(n(7, false), n(6, true), n(5, false)));
        assertEquals(1, baru.size());
        assertEquals(7, baru.get(0).id);

        assertTrue(
                "yang sudah tampil tidak diulang",
                CatatanNotifikasi.ambilBaru(c, respons(n(7, false))).isEmpty());
    }

    private static NotifikasiResponse respons(Notifikasi... isi) {
        NotifikasiResponse r = new NotifikasiResponse();
        r.data = new ArrayList<>(List.of(isi));
        return r;
    }

    private static Notifikasi n(long id, boolean dibaca) {
        Notifikasi n = new Notifikasi();
        n.id = id;
        n.isRead = dibaca;
        n.type = NotifikasiSistem.JENIS_WAR;
        n.title = "War dimulai!";
        return n;
    }
}
