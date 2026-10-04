package id.lifeoffoods.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import id.lifeoffoods.data.api.model.TokoDetailDto;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ProfilTokoTest {

    @Test
    public void hariBerurutanDenganJamSamaDigabung() {
        List<TokoDetailDto.JamToko> data = new ArrayList<>();
        for (int hari = 1; hari <= 5; hari++) {
            data.add(jam(hari, "07:00", "21:00"));
        }
        data.add(jam(6, "08:00", "22:00"));
        data.add(jam(0, "08:00", "22:00"));

        List<ProfilToko.BarisJam> baris = ProfilToko.ringkasJam(data);

        assertEquals(2, baris.size());
        assertEquals("Senin sampai Jumat", baris.get(0).hari);
        assertEquals("07.00 sampai 21.00", baris.get(0).jam);
        assertEquals("Sabtu dan Minggu", baris.get(1).hari);
        assertEquals("08.00 sampai 22.00", baris.get(1).jam);
    }

    @Test
    public void hariTutupDanHariTanpaDataJadiTutup() {
        List<TokoDetailDto.JamToko> data = new ArrayList<>();
        for (int hari = 1; hari <= 6; hari++) {
            data.add(jam(hari, "08:00", "17:00"));
        }
        TokoDetailDto.JamToko minggu = new TokoDetailDto.JamToko();
        minggu.dayOfWeek = 0;
        minggu.isClosed = true;
        data.add(minggu);
        data.remove(2); // Rabu tidak ada di data

        List<ProfilToko.BarisJam> baris = ProfilToko.ringkasJam(data);

        assertEquals(4, baris.size());
        assertEquals("Senin dan Selasa", baris.get(0).hari);
        assertEquals("Rabu", baris.get(1).hari);
        assertNull(baris.get(1).jam);
        assertEquals("Kamis sampai Sabtu", baris.get(2).hari);
        assertEquals("Minggu", baris.get(3).hari);
        assertNull(baris.get(3).jam);
    }

    @Test
    public void tanpaJamOperasionalDaftarKosong() {
        assertTrue(ProfilToko.ringkasJam(null).isEmpty());
        assertTrue(ProfilToko.ringkasJam(new ArrayList<>()).isEmpty());
    }

    @Test
    public void nomorHpDitampilkanFormatLokal() {
        assertEquals("0812-9988-7766", ProfilToko.nomorHp("6281299887766"));
        assertEquals("0813-2244-556", ProfilToko.nomorHp("628132244556"));
        assertEquals("", ProfilToko.nomorHp(null));
        assertEquals("12345", ProfilToko.nomorHp("12345"));
    }

    @Test
    public void ratingMemakaiKomaDanHilangTanpaUlasan() {
        assertEquals("4,8 (180)", ProfilToko.rating(4.8, 180));
        assertEquals("5,0 (1)", ProfilToko.rating(5.0, 1));
        assertNull(ProfilToko.rating(null, 0));
        assertNull(ProfilToko.rating(4.0, 0));
    }

    private static TokoDetailDto.JamToko jam(int hari, String buka, String tutup) {
        TokoDetailDto.JamToko j = new TokoDetailDto.JamToko();
        j.dayOfWeek = hari;
        j.openTime = buka;
        j.closeTime = tutup;
        return j;
    }
}
