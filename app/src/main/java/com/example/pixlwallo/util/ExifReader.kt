package com.example.pixlwallo.util

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

data class ExifInfo(
    val camera: String? = null,
    val model: String? = null,
    val make: String? = null,
    val dateTime: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val width: Int? = null,
    val height: Int? = null,
    val orientation: Int? = null,
    val iso: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val flash: String? = null
)

object ExifReader {
    suspend fun readExif(context: Context, uri: Uri): ExifInfo? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                val camera = if (make != null && model != null) {
                    "$make $model"
                } else {
                    make ?: model
                }
                
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                
                val latLong = FloatArray(2)
                val hasLocation = exif.getLatLong(latLong)
                val latitude = if (hasLocation) latLong[0].toDouble() else null
                val longitude = if (hasLocation) latLong[1].toDouble() else null
                
                val location = if (hasLocation) {
                    String.format("%.6f, %.6f", latitude, longitude)
                } else {
                    null
                }
                
                val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).let {
                    if (it > 0) it else null
                }
                val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).let {
                    if (it > 0) it else null
                }
                
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                
                val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                    ?: exif.getAttribute(ExifInterface.TAG_ISO)
                
                val aperture = try {
                    val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                    if (fNumber > 0) "f/${fNumber}" else null
                } catch (e: Exception) {
                    exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
                }
                
                val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
                    try {
                        val expTime = it.toDouble()
                        if (expTime < 1) {
                            "${(1 / expTime).toInt()}/1"
                        } else {
                            "${expTime}s"
                        }
                    } catch (e: Exception) {
                        it
                    }
                }
                
                val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                    try {
                        val focal = it.toDouble()
                        "${focal.toInt()}mm"
                    } catch (e: Exception) {
                        it
                    }
                }
                
                val flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1).let {
                    when (it) {
                        0x0 -> "未使用闪光灯"
                        0x1 -> "使用闪光灯"
                        0x5 -> "闪光灯未触发"
                        0x7 -> "闪光灯已触发"
                        else -> null
                    }
                }
                
                ExifInfo(
                    camera = camera,
                    model = model,
                    make = make,
                    dateTime = dateTime,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    width = width,
                    height = height,
                    orientation = orientation,
                    iso = iso,
                    aperture = aperture,
                    exposureTime = exposureTime,
                    focalLength = focalLength,
                    flash = flash
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

