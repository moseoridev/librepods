package me.kavishdevar.librepods.presentation.overlays

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class VideoInfo(val ratio: Float, val background: Int)
private object VideoInfoCache {
    private val entries = linkedMapOf<String, VideoInfo>()
    fun read(context: Context, resource: Int): VideoInfo? = synchronized(entries) {
        val key = "$resource:${context.resources.configuration.uiMode}"
        entries[key]?.let { return it }
        val retriever = try { MediaMetadataRetriever() } catch (_: Exception) { return null }
        try {
            context.resources.openRawResourceFd(resource).use { fd ->
                retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: return null
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: return null
            if (width <= 0 || height <= 0) return null
            val first = retriever.getFrameAtTime(0)
            val background = try { first?.getPixel(0, 0) ?: android.graphics.Color.TRANSPARENT } finally { first?.recycle() }
            val info = VideoInfo(width / height, background)
            if (entries.size >= 4) entries.remove(entries.keys.first())
            entries[key] = info
            info
        } catch (_: Exception) {
            null
        } finally { retriever.release() }
    }
}

/** Metadata fixes geometry before MediaPlayer prepares; artwork covers loading/failure. */
@Composable
internal fun ConnectionVideo(@RawRes resource: Int, @DrawableRes fallback: Int, loadingColor: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var ready by remember(resource, context) { mutableStateOf(false) }
    var failed by remember(resource, context) { mutableStateOf(false) }
    var checked by remember(resource, context) { mutableStateOf(false) }
    val info by produceState<VideoInfo?>(null, resource, context) {
        value = withContext(Dispatchers.IO) { if (resource == 0) null else VideoInfoCache.read(context, resource) }
        checked = true
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val geometry = info
        if (failed || (checked && geometry == null)) Image(painterResource(fallback), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else if (!ready) {
            val cover = if (geometry == null) Modifier.fillMaxSize().background(loadingColor)
                else Modifier.fillMaxWidth().aspectRatio(geometry.ratio).background(Color(geometry.background))
            Box(cover)
        }
        if (geometry != null && !failed) key(resource, context) {
            AndroidView(factory = { ctx -> OneShotVideoView(ctx, resource,
                    rendered = { ready = true }, failed = { failed = true }) },
                modifier = Modifier.fillMaxWidth().aspectRatio(geometry.ratio)
                    .background(Color(geometry.background)).alpha(if (ready) 1f else 0f),
                onRelease = { it.release() })
        }
    }
}

/** No looping, seek-to-start polling, or retained Surface after removal. */
private class OneShotVideoView(context: Context, private val resource: Int,
    private val rendered: () -> Unit, private val failed: () -> Unit) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    init { isOpaque = false; surfaceTextureListener = this }
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        release()
        try {
            surface = Surface(texture)
            val mp = MediaPlayer()
            player = mp
            mp.apply {
                mp.setDataSource(context, Uri.parse("android.resource://${context.packageName}/$resource"))
                mp.setSurface(surface)
                mp.isLooping = false
                mp.setVolume(0f, 0f)
                mp.setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && player === mp) rendered()
                    false
                }
                mp.setOnPreparedListener { if (player === it) it.start() }
                mp.setOnErrorListener { _, _, _ -> failed(); release(); true }
                mp.prepareAsync()
            }
        } catch (_: Exception) { failed(); release() }
    }
    fun release() {
        val previous = player
        player = null
        previous?.setOnPreparedListener(null)
        previous?.setOnInfoListener(null)
        previous?.setOnErrorListener(null)
        previous?.release()
        surface?.release()
        surface = null
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { release(); return true }
}
