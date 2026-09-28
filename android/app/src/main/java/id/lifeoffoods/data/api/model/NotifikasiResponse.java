package id.lifeoffoods.data.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * GET /notifications. Baru {@code unread_count} yang dipakai (titik di lonceng K07); isi daftar
 * ditambahkan saat K17 dikerjakan.
 */
public class NotifikasiResponse {

    @SerializedName("unread_count")
    public int unreadCount;
}
