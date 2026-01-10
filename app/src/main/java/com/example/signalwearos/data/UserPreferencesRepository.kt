package com.example.signalwearos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    
    private val IS_DEVICE_LINKED = booleanPreferencesKey("is_device_linked")
    private val IDENTITY_KEY_PUBLIC = stringPreferencesKey("identity_key_public")
    private val IDENTITY_KEY_PRIVATE = stringPreferencesKey("identity_key_private")

    val isDeviceLinked: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DEVICE_LINKED] ?: false
        }

    suspend fun setDeviceLinked(isLinked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DEVICE_LINKED] = isLinked
        }
    }
    
    suspend fun saveIdentityKey(publicKey: String, privateKey: String) {
        context.dataStore.edit { preferences ->
            preferences[IDENTITY_KEY_PUBLIC] = publicKey
            preferences[IDENTITY_KEY_PRIVATE] = privateKey
        }
    }
    
    val identityKeyPublic: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[IDENTITY_KEY_PUBLIC]
        }
        
    val identityKeyPrivate: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[IDENTITY_KEY_PRIVATE]
        }
}
