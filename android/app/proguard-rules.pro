# Minify belum dinyalakan (lihat app/build.gradle.kts). Kalau dinyalakan, model Gson di
# id.lifeoffoods.data.api.model wajib dipertahankan karena dibaca lewat refleksi.
-keep class id.lifeoffoods.data.api.model.** { *; }
