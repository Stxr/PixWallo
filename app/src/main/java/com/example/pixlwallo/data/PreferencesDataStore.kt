package com.example.pixlwallo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

object PreferencesDataStore {
    private const val STORE_NAME = "pixlwallo_prefs"
    @Volatile private var instance: DataStore<Preferences>? = null

    fun create(context: Context): DataStore<Preferences> {
        val existing = instance
        if (existing != null) return existing
        return synchronized(this) {
            val again = instance
            if (again != null) again else PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob()),
                produceFile = { context.applicationContext.preferencesDataStoreFile(STORE_NAME) }
            ).also { instance = it }
        }
    }
}