package moe.ouom.neriplayer.core.startup.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.util.platform.isTvDevice

internal object AppImageLoaderInitializer {
    private const val IMAGE_CACHE_DIRECTORY_NAME = "image_cache"

    fun initialize(app: Application) {
        val isTvDevice = app.isTvDevice()
        if (isTvDevice) {
            // TV: 禁用图片磁盘缓存, 清理历史缓存目录
            runCatching { app.cacheDir.resolve(IMAGE_CACHE_DIRECTORY_NAME).deleteRecursively() }
        }
        val builder = ImageLoader.Builder(app)
            .okHttpClient { AppContainer.sharedOkHttpClient }
            .respectCacheHeaders(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(app)
                    .maxSizePercent(0.12)
                    .build()
            }
        val imageLoader = if (isTvDevice) {
            builder
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        } else {
            builder
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCache {
                    DiskCache.Builder()
                        .directory(app.cacheDir.resolve(IMAGE_CACHE_DIRECTORY_NAME))
                        .maxSizePercent(0.02)
                        .build()
                }
                .build()
        }
        Coil.setImageLoader(imageLoader)
    }
}
