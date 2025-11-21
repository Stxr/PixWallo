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
                    ?: exif.getAttribute(ExifInterface.TAG_RW2_ISO)

                val aperture = try {
                    // 先尝试获取字符串格式（可能是 Rational 格式如 "35/10"）
                    val fNumberStr = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                    if (fNumberStr != null) {
                        // 尝试解析 Rational 格式
                        val fNumber = if (fNumberStr.contains("/")) {
                            val parts = fNumberStr.split("/")
                            if (parts.size == 2) {
                                val num = parts[0].toDoubleOrNull() ?: 0.0
                                val den = parts[1].toDoubleOrNull() ?: 1.0
                                if (den > 0) num / den else 0.0
                            } else {
                                fNumberStr.toDoubleOrNull() ?: 0.0
                            }
                        } else {
                            fNumberStr.toDoubleOrNull() ?: 0.0
                        }
                        if (fNumber > 0) {
                            // 格式化光圈值，保留一位小数
                            if (fNumber % 1.0 == 0.0) {
                                "f/${fNumber.toInt()}"
                            } else {
                                String.format("f/%.1f", fNumber)
                            }
                        } else {
                            null
                        }
                    } else {
                        // 如果字符串格式获取失败，尝试直接获取 double
                        val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                        if (fNumber > 0) {
                            if (fNumber % 1.0 == 0.0) {
                                "f/${fNumber.toInt()}"
                            } else {
                                String.format("f/%.1f", fNumber)
                            }
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
                
                val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { expTimeStr ->
                    try {
                        // 尝试解析 Rational 格式（如 "1/60"）
                        val expTime = if (expTimeStr.contains("/")) {
                            val parts = expTimeStr.split("/")
                            if (parts.size == 2) {
                                val num = parts[0].toDoubleOrNull() ?: 0.0
                                val den = parts[1].toDoubleOrNull() ?: 1.0
                                if (den > 0) num / den else 0.0
                            } else {
                                expTimeStr.toDoubleOrNull() ?: 0.0
                            }
                        } else {
                            expTimeStr.toDoubleOrNull() ?: 0.0
                        }
                        
                        if (expTime > 0) {
                            if (expTime < 1.0) {
                                "1/${kotlin.math.round(1.0 / expTime).toInt()}"
                            } else {
                                "${expTime}s"
                            }
                        } else {
                            expTimeStr
                        }
                    } catch (e: Exception) {
                        expTimeStr
                    }
                }
                
                val focalLength = try {
                    // 优先尝试获取等效焦距 (35mm format)
                    val focal35mm = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
                    if (focal35mm > 0) {
                        "${focal35mm}mm"
                    } else {
                        // 如果没有等效焦距，使用物理焦距
                        val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                        if (focal > 0) {
                            // 如果是整数，显示整数；否则保留一位小数
                            if (focal % 1.0 == 0.0) {
                                "${focal.toInt()}mm"
                            } else {
                                String.format("%.1fmm", focal)
                            }
                        } else {
                            exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                        }
                    }
                } catch (e: Exception) {
                    exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
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

