package com.motorola.launcher.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hardware-accelerated Video Player for 3D Character Companions (Mithra, Mithra's Mom).
 * Uses Android TextureView with MediaPlayer for seamless Compose clipping, rounded corners,
 * holographic border rendering, and zero-stutter continuous video loop playback.
 */
@Composable
fun VideoCompanionPlayer(
    modifier: Modifier = Modifier,
    @RawRes rawResId: Int? = null,
    videoUri: Uri? = null,
    isMuted: Boolean = false,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = Color(0xFF00E5FF),
    onPrepared: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(rawResId, videoUri) {
        onDispose {
            try {
                mediaPlayer?.run {
                    if (isPlaying) stop()
                    reset()
                    release()
                }
            } catch (_: Throwable) {}
            mediaPlayer = null
        }
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(12.dp, shape)
            .clip(shape)
            .background(Color.Black)
            .border(1.5.dp, borderColor, shape)
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            try {
                                mediaPlayer?.release()
                                val surface = Surface(surfaceTexture)
                                val player = MediaPlayer().apply {
                                    setSurface(surface)
                                    if (videoUri != null) {
                                        setDataSource(ctx, videoUri)
                                    } else if (rawResId != null) {
                                        val afd = ctx.resources.openRawResourceFd(rawResId)
                                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                        afd.close()
                                    }
                                    isLooping = true
                                    val vol = if (isMuted) 0f else 1f
                                    setVolume(vol, vol)
                                    prepareAsync()
                                    setOnPreparedListener { mp ->
                                        mp.start()
                                        onPrepared?.invoke()
                                    }
                                }
                                mediaPlayer = player
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            try {
                                mediaPlayer?.run {
                                    if (isPlaying) stop()
                                    reset()
                                    release()
                                }
                            } catch (_: Throwable) {}
                            mediaPlayer = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.matchParentSize()
        )
    }
}
