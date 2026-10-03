package id.lifeoffoods.ui.umum;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

/**
 * Foto jualan di atas komponen "Slot foto". ImageView-nya disembunyikan selama tidak ada foto atau
 * foto gagal dimuat, sehingga isi slot (ikon + label "Foto") tetap terlihat sebagai cadangan.
 */
public final class FotoJualan {

    private FotoJualan() {}

    /** Sudut sama di keempat sisi, dalam dp; 0 untuk foto penuh tanpa sudut. */
    public static void muat(ImageView v, @Nullable String url, float sudutDp) {
        muat(v, url, sudutDp, sudutDp);
    }

    /** Sudut atas dan bawah terpisah, misalnya foto di bagian atas kartu. */
    public static void muat(ImageView v, @Nullable String url, float atasDp, float bawahDp) {
        if (url == null || url.isEmpty()) {
            Glide.with(v).clear(v);
            v.setVisibility(View.GONE);
            return;
        }
        float d = v.getResources().getDisplayMetrics().density;
        float atas = atasDp * d;
        float bawah = bawahDp * d;
        v.setVisibility(View.VISIBLE);
        Glide.with(v)
                .load(url)
                .transform(new CenterCrop(), new GranularRoundedCorners(atas, atas, bawah, bawah))
                .listener(
                        new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    @Nullable Object model,
                                    @NonNull Target<Drawable> target,
                                    boolean isFirstResource) {
                                v.setVisibility(View.GONE);
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(
                                    @NonNull Drawable resource,
                                    @NonNull Object model,
                                    Target<Drawable> target,
                                    @NonNull DataSource dataSource,
                                    boolean isFirstResource) {
                                return false;
                            }
                        })
                .into(v);
    }
}
