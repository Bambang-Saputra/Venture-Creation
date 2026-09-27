package id.lifeoffoods.ui.umum;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

/**
 * Adapter RecyclerView generik untuk semua daftar (ADR-0002). Satu layar cukup menentukan cara
 * membuat binding baris, cara mengisinya, dan kunci item. Panduan: docs/panduan/pola-layar-java.md.
 *
 * <pre>
 * BaseListAdapter&lt;Listing, ItemListingBinding&gt; adapter = new BaseListAdapter&lt;&gt;(
 *         ItemListingBinding::inflate,
 *         (b, item) -&gt; b.judul.setText(item.title),
 *         item -&gt; item.id);
 * adapter.submitList(daftar);
 * </pre>
 *
 * @param <T> tipe item
 * @param <VB> ViewBinding baris
 */
public class BaseListAdapter<T, VB extends ViewBinding>
        extends ListAdapter<T, BaseListAdapter.Holder<VB>> {

    /** Sama dengan tanda tangan XxxBinding.inflate(LayoutInflater, ViewGroup, boolean). */
    public interface Pembuat<VB> {
        VB buat(LayoutInflater inflater, ViewGroup parent, boolean attach);
    }

    public interface Pengisi<T, VB> {
        void isi(VB binding, T item);
    }

    public interface Kunci<T> {
        Object dari(T item);
    }

    public interface SamaIsi<T> {
        boolean sama(T lama, T baru);
    }

    private final Pembuat<VB> pembuat;
    private final Pengisi<T, VB> pengisi;

    /**
     * Tanpa pembanding isi: baris dengan kunci sama selalu diisi ulang. Cocok untuk model API yang
     * tidak meng-override equals, karena daftar baru dari server memang harus tampil.
     */
    public BaseListAdapter(Pembuat<VB> pembuat, Pengisi<T, VB> pengisi, Kunci<T> kunci) {
        this(pembuat, pengisi, kunci, (lama, baru) -> false);
    }

    public BaseListAdapter(
            Pembuat<VB> pembuat, Pengisi<T, VB> pengisi, Kunci<T> kunci, SamaIsi<T> samaIsi) {
        super(
                new DiffUtil.ItemCallback<T>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull T lama, @NonNull T baru) {
                        return kunci.dari(lama).equals(kunci.dari(baru));
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull T lama, @NonNull T baru) {
                        return samaIsi.sama(lama, baru);
                    }
                });
        this.pembuat = pembuat;
        this.pengisi = pengisi;
    }

    @NonNull
    @Override
    public Holder<VB> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder<>(pembuat.buat(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder<VB> holder, int position) {
        pengisi.isi(holder.binding, getItem(position));
    }

    public static class Holder<VB extends ViewBinding> extends RecyclerView.ViewHolder {
        public final VB binding;

        Holder(VB binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
