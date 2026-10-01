package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import id.lifeoffoods.data.api.model.JualanMitraDto;
import id.lifeoffoods.data.api.model.LaporanMingguanDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class DasborMitraTest {

    @Test
    public void sisaWaktuJamPenuhAtauMenit() {
        DasborMitra.SisaWaktu dua = DasborMitra.sisaWaktu(150);
        assertTrue(dua.jam);
        assertEquals(2, dua.angka);
        DasborMitra.SisaWaktu menit = DasborMitra.sisaWaktu(45);
        assertFalse(menit.jam);
        assertEquals(45, menit.angka);
        assertTrue(DasborMitra.sisaWaktu(60).jam);
    }

    @Test
    public void sisaWaktuKosongKalauSudahTutup() {
        assertNull(DasborMitra.sisaWaktu(null));
        assertNull(DasborMitra.sisaWaktu(0));
    }

    @Test
    public void adaCatatanHanyaKalauAdaNilai() {
        assertFalse(DasborMitra.adaCatatan(null));
        assertFalse(DasborMitra.adaCatatan(Arrays.asList(hari(null), hari(null))));
        assertTrue(DasborMitra.adaCatatan(Arrays.asList(hari(null), hari(0L))));
    }

    @Test
    public void tasHariIniUrutStatusTanpaMenuMaksTiga() {
        List<JualanMitraDto> d =
                DasborMitra.tasHariIni(
                        Arrays.asList(
                                tas(1, JualanMitraDto.HABIS),
                                tas(2, JualanMitraDto.LEWAT),
                                menu(3),
                                tas(4, JualanMitraDto.AKTIF),
                                tas(5, JualanMitraDto.DIJEDA),
                                tas(6, JualanMitraDto.AKTIF)));
        assertEquals(DasborMitra.TAS_MAKS, d.size());
        assertEquals(4, d.get(0).id);
        assertEquals(6, d.get(1).id);
        assertEquals(5, d.get(2).id);
    }

    @Test
    public void tasHariIniKosong() {
        assertTrue(DasborMitra.tasHariIni(null).isEmpty());
        assertTrue(DasborMitra.tasHariIni(Collections.singletonList(menu(1))).isEmpty());
    }

    private static LaporanMingguanDto.Harian hari(Long nilai) {
        LaporanMingguanDto.Harian h = new LaporanMingguanDto.Harian();
        h.date = "2026-09-28";
        h.wastedValueRupiah = nilai;
        return h;
    }

    private static JualanMitraDto tas(long id, String status) {
        JualanMitraDto j = new JualanMitraDto();
        j.id = id;
        j.type = JualanMitraDto.TIPE_TAS;
        j.status = status;
        return j;
    }

    private static JualanMitraDto menu(long id) {
        JualanMitraDto j = tas(id, JualanMitraDto.AKTIF);
        j.type = JualanMitraDto.TIPE_MENU;
        return j;
    }
}
