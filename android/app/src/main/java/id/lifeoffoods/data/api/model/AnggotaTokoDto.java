package id.lifeoffoods.data.api.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Satu baris GET /partner/stores/{store}/members (M15). Baris pertama selalu pemilik. */
public class AnggotaTokoDto {

    /** null untuk baris pemilik toko, yang tidak bisa dicabut. */
    @Nullable
    @SerializedName("id")
    public Long id;

    @SerializedName("name")
    public String name;

    /** Format 628..., ditampilkan sebagai 0812-... */
    @SerializedName("phone")
    public String phone;

    /** owner atau cashier. */
    @SerializedName("role")
    public String role;

    @SerializedName("is_store_owner")
    public boolean isStoreOwner;

    /** Hanya di respons POST: teks undangan yang dibagikan lewat WhatsApp. */
    @Nullable
    @SerializedName("invite_message")
    public String inviteMessage;

    public boolean bisaDicabut() {
        return !isStoreOwner && id != null;
    }

    /** POST /partner/stores/{store}/members. */
    public static class Undang {

        @SerializedName("phone")
        public final String phone;

        @SerializedName("name")
        public final String name;

        public Undang(String phone, String name) {
            this.phone = phone;
            this.name = name;
        }
    }
}
