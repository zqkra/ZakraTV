package com.zakratv.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.zakratv.app.data.local.PrefsStore
import com.zakratv.app.data.remote.HttpClientFactory
import com.zakratv.app.data.remote.RealDebridApi
import com.zakratv.app.data.remote.StreamProvider
import com.zakratv.app.data.remote.TmdbApi
import com.zakratv.app.data.repository.CatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
class ZakraApp : Application(), ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var prefs: PrefsStore
        private set
    lateinit var realDebrid: RealDebridApi
        private set
    lateinit var repository: CatalogRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PrefsStore(this)
        realDebrid = RealDebridApi()
        val tmdb = TmdbApi()
        val streams = StreamProvider(realDebrid)
        repository = CatalogRepository(tmdb, realDebrid, streams, prefs)

        // Seed RD token from BuildConfig → no first-run setup when pre-supplied
        appScope.launch {
            val token = prefs.ensureDefaultRdToken()
            if (token.isNotBlank()) {
                realDebrid.setToken(token)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        // Memory-tight Coil for low-end Android TV / Fire Stick
        return ImageLoader.Builder(this)
            .okHttpClient {
                HttpClientFactory.client.newBuilder()
                    .cache(null)
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.12)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(40L * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        lateinit var instance: ZakraApp
            private set
    }
}
