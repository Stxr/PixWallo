package com.example.pixlwallo.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Size
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.example.pixlwallo.model.ApplyScope

class WallpaperApplier(private val context: Context) {
    private val wm = WallpaperManager.getInstance(context)
    private val loader = ImageLoader(context)

    suspend fun setFromUri(uri: Uri, scope: ApplyScope) {
        val target = desiredSize()
        val bmp = loadCroppedBitmap(uri, target)
        when (scope) {
            ApplyScope.LOCK -> wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
            ApplyScope.SYSTEM -> wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
            ApplyScope.BOTH -> {
                wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
                wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
            }
        }
    }

    private fun desiredSize(): Size {
        val w = wm.desiredMinimumWidth
        val h = wm.desiredMinimumHeight
        return if (w > 0 && h > 0) Size(w, h) else Size(1080, 1920)
    }

    private suspend fun loadCroppedBitmap(uri: Uri, size: Size): Bitmap {
        val req = ImageRequest.Builder(context)
            .data(uri)
            .size(size.width, size.height)
            .scale(Scale.FILL)
            .allowHardware(false)
            .build()
        val res = loader.execute(req)
        val d: Drawable = res.drawable
            ?: Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888).let { BitmapDrawable(context.resources, it) }
        return d.toBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    }
}