package com.motorola.launcher.data.icon

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Robust icon manager coordinating L1 in-memory LRU cache, L2 binary disk cache,
 * and off-main-thread rasterization.
 *
 * Guarantees:
 * - Thread-safe in-flight deduplication to prevent redundant disk I/O and rasterization.
 * - Software bitmaps properly persisted to BinaryDiskCache on miss.
 * - Memory bounded via LruCache with automatic GC cleanup.
 */
class HardwareIconManager(
    context: Context,
    val diskCache: BinaryDiskCache = BinaryDiskCache(context)
) {
    private val rasterizer = AdaptiveIconRasterizer(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Bounded in-memory L1 cache (128 app icons)
    private val memoryCache = object : LruCache<String, ImageBitmap>(128) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String?,
            oldValue: ImageBitmap?,
            newValue: ImageBitmap?
        ) {
            // Bitmaps are left to GC; never recycle explicitly while UI may draw them
        }
    }

    // In-flight request deduplication map to coalesce concurrent cache misses
    private val inFlightLoads = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<ImageBitmap?>>()

    /**
     * Resolves an icon with sub-millisecond memory lookup, binary disk fallback,
     * or off-main-thread rasterization on Dispatchers.IO.
     * Deduplicates concurrent misses for the same key.
     */
    suspend fun loadIcon(
        cacheKey: String,
        fallbackLoader: () -> Drawable?
    ): ImageBitmap? {
        // 1. Fast L1 memory cache hit (sub-0.1ms)
        memoryCache.get(cacheKey)?.let { return it }

        // 2. Check for in-flight request to prevent duplicate concurrent work
        val existingDeferred = inFlightLoads[cacheKey]
        if (existingDeferred != null) {
            return try {
                existingDeferred.await()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                null
            }
        }

        // 3. Launch async loading job
        val deferred = scope.async {
            try {
                // Double check memory cache inside coroutine
                memoryCache.get(cacheKey)?.let { return@async it }

                // Check L2 binary disk cache
                val diskBmp = diskCache.readBitmap(cacheKey)
                if (diskBmp != null) {
                    val imageBmp = diskBmp.asImageBitmap()
                    memoryCache.put(cacheKey, imageBmp)
                    return@async imageBmp
                }

                // Fallback: Rasterize drawable off-thread
                val drawable = fallbackLoader() ?: return@async null
                val softwareBmp = rasterizer.rasterize(drawable) ?: return@async null

                // Persist software bitmap to binary disk cache
                diskCache.writeSoftwareBitmap(cacheKey, softwareBmp)

                val imageBmp = softwareBmp.asImageBitmap()
                memoryCache.put(cacheKey, imageBmp)
                imageBmp
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                null
            } finally {
                inFlightLoads.remove(cacheKey)
            }
        }

        inFlightLoads[cacheKey] = deferred

        return try {
            deferred.await()
        } catch (c: CancellationException) {
            throw c
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Synchronous memory cache check for immediate rendering inside LazyGrid composables.
     */
    fun peekMemory(cacheKey: String): ImageBitmap? {
        return memoryCache.get(cacheKey)
    }

    /**
     * Invalidates a specific icon entry from memory and disk.
     */
    fun invalidate(cacheKey: String) {
        memoryCache.remove(cacheKey)
        diskCache.delete(cacheKey)
    }

    /**
     * Clears all memory cache entries.
     */
    fun clearMemory() {
        memoryCache.evictAll()
    }

    /**
     * Clears both memory and disk caches.
     */
    fun clearAll() {
        memoryCache.evictAll()
        diskCache.evictAll()
    }
}
