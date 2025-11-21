package com.example.pixlwallo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.pixlwallo.model.ApplyScope
import com.example.pixlwallo.model.ExifPosition
import com.example.pixlwallo.model.PlaybackConfig
import com.example.pixlwallo.model.PlaybackOrder
import com.example.pixlwallo.model.ImgDisplayMode
import com.example.pixlwallo.model.OrientationFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(context: Context) {
    private val store: DataStore<Preferences> = PreferencesDataStore.create(context)

    private object Keys {
        val perItemMs = longPreferencesKey("per_item_ms")
        val maxDurationMs = longPreferencesKey("max_duration_ms")
        val idleStopMs = longPreferencesKey("idle_stop_ms")
        val order = stringPreferencesKey("playback_order")
        val scope = stringPreferencesKey("apply_scope")
        val enableExifTap = booleanPreferencesKey("enable_exif_tap")
        val exifPosition = stringPreferencesKey("exif_position")
        val scaleType = stringPreferencesKey("scale_type")
        val orientationFilter = stringPreferencesKey("orientation_filter")
    }

    val configFlow: Flow<PlaybackConfig> = store.data.map { p ->
        PlaybackConfig(
            perItemMs = p[Keys.perItemMs] ?: 10_000,
            maxDurationMs = (p[Keys.maxDurationMs] ?: -1L).let { if (it <= 0) null else it },
            idleStopMs = (p[Keys.idleStopMs] ?: 30 * 60_000).let { if (it <= 0) null else it },
            order = when (p[Keys.order]) {
                PlaybackOrder.RANDOM.name -> PlaybackOrder.RANDOM
                else -> PlaybackOrder.SELECTED
            },
            applyScope = when (p[Keys.scope]) {
                ApplyScope.SYSTEM.name -> ApplyScope.SYSTEM
                ApplyScope.BOTH.name -> ApplyScope.BOTH
                else -> ApplyScope.LOCK
            },
            enableExifTap = p[Keys.enableExifTap] ?: true,
            exifPosition = when (p[Keys.exifPosition]) {
                ExifPosition.TOP_LEFT.name -> ExifPosition.TOP_LEFT
                ExifPosition.TOP_RIGHT.name -> ExifPosition.TOP_RIGHT
                ExifPosition.BOTTOM_LEFT.name -> ExifPosition.BOTTOM_LEFT
                ExifPosition.CENTER.name -> ExifPosition.CENTER
                else -> ExifPosition.BOTTOM_RIGHT
            },
            imgDisplayMode = when (p[Keys.scaleType]) {
                ImgDisplayMode.FIT_CENTER.name -> ImgDisplayMode.FIT_CENTER
                ImgDisplayMode.SMART.name -> ImgDisplayMode.SMART
                else -> ImgDisplayMode.FILL_SCREEN
            }
        )
    }

    val orientationFilterFlow: Flow<OrientationFilter> = store.data.map { p ->
        when (p[Keys.orientationFilter]) {
            OrientationFilter.LANDSCAPE.name -> OrientationFilter.LANDSCAPE
            OrientationFilter.PORTRAIT.name -> OrientationFilter.PORTRAIT
            else -> OrientationFilter.ALL
        }
    }

    suspend fun setOrientationFilter(filter: OrientationFilter) {
        store.edit { p ->
            p[Keys.orientationFilter] = filter.name
        }
    }

    suspend fun update(block: (PlaybackConfig) -> PlaybackConfig) {
        store.edit { p ->
            val current = PlaybackConfig(
                perItemMs = p[Keys.perItemMs] ?: 10_000,
                maxDurationMs = (p[Keys.maxDurationMs] ?: -1L).let { if (it <= 0) null else it },
                idleStopMs = (p[Keys.idleStopMs] ?: 30 * 60_000).let { if (it <= 0) null else it },
                order = when (p[Keys.order]) {
                    PlaybackOrder.RANDOM.name -> PlaybackOrder.RANDOM
                    else -> PlaybackOrder.SELECTED
                },
                applyScope = when (p[Keys.scope]) {
                    ApplyScope.SYSTEM.name -> ApplyScope.SYSTEM
                    ApplyScope.BOTH.name -> ApplyScope.BOTH
                    else -> ApplyScope.LOCK
                },
                enableExifTap = p[Keys.enableExifTap] ?: true,
                exifPosition = when (p[Keys.exifPosition]) {
                    ExifPosition.TOP_LEFT.name -> ExifPosition.TOP_LEFT
                    ExifPosition.TOP_RIGHT.name -> ExifPosition.TOP_RIGHT
                    ExifPosition.BOTTOM_LEFT.name -> ExifPosition.BOTTOM_LEFT
                    ExifPosition.CENTER.name -> ExifPosition.CENTER
                    else -> ExifPosition.BOTTOM_RIGHT
                },
                imgDisplayMode = when (p[Keys.scaleType]) {
                    ImgDisplayMode.FIT_CENTER.name -> ImgDisplayMode.FIT_CENTER
                    ImgDisplayMode.SMART.name -> ImgDisplayMode.SMART
                    else -> ImgDisplayMode.FILL_SCREEN
                }
            )
            val next = block(current)
            p[Keys.perItemMs] = next.perItemMs
            p[Keys.maxDurationMs] = next.maxDurationMs ?: -1
            p[Keys.idleStopMs] = next.idleStopMs ?: -1
            p[Keys.order] = next.order.name
            p[Keys.scope] = next.applyScope.name
            p[Keys.enableExifTap] = next.enableExifTap
            p[Keys.exifPosition] = next.exifPosition.name
            p[Keys.scaleType] = next.imgDisplayMode.name
        }
    }
}