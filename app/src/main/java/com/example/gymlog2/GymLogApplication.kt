package com.example.gymlog2

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import java.util.concurrent.Executors

class GymLogApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(GifDecoder.Factory())
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        backgroundSync(this)
    }

    private fun backgroundSync(context: Context) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val prefs = context.getSharedPreferences("theme_prefs", MODE_PRIVATE)
                val isLoggedIn = prefs.getBoolean("logged_in", false)
                if (!isLoggedIn) return@execute

                val profilePrefs = context.getSharedPreferences("user_profiles", MODE_PRIVATE)
                val userId = profilePrefs.getString("own_user_id", null) ?: return@execute
                val name = profilePrefs.getString("own_name", "") ?: ""
                val photo = profilePrefs.getString("own_photo", "") ?: ""
                if (name.isBlank() || userId == "local_user") return@execute

                val db = AppDatabase.getDatabase(context)
                SocialRepository(db).syncUserProfileBlocking(userId, name, photo)
            } catch (_: Exception) {}
        }
    }
}
