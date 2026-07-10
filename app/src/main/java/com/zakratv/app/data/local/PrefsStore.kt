package com.zakratv.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zakratv.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zakra_prefs")

class PrefsStore(private val context: Context) {

    private val keyRdToken = stringPreferencesKey("rd_token")
    private val keyMyList = stringSetPreferencesKey("my_list")

    val rdTokenFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[keyRdToken].orEmpty()
    }

    val myListFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[keyMyList] ?: emptySet()
    }

    /**
     * Seeds BuildConfig token on first launch so Real-Debrid works without setup UI.
     * Returns the effective token (prefs or default).
     */
    suspend fun ensureDefaultRdToken(): String {
        val current = context.dataStore.data.first()[keyRdToken].orEmpty()
        if (current.isNotBlank()) return current
        val baked = BuildConfig.REAL_DEBRID_TOKEN.trim()
        if (baked.isNotBlank()) {
            context.dataStore.edit { it[keyRdToken] = baked }
            return baked
        }
        return ""
    }

    suspend fun getRdToken(): String {
        val fromPrefs = context.dataStore.data.first()[keyRdToken].orEmpty()
        if (fromPrefs.isNotBlank()) return fromPrefs
        return BuildConfig.REAL_DEBRID_TOKEN.trim()
    }

    suspend fun setRdToken(token: String) {
        context.dataStore.edit { it[keyRdToken] = token.trim() }
    }

    suspend fun clearRdToken() {
        context.dataStore.edit { it.remove(keyRdToken) }
    }

    /** Keys like "movie:123" or "tv:456". */
    suspend fun isInMyList(key: String): Boolean {
        val set = context.dataStore.data.first()[keyMyList] ?: emptySet()
        return key in set
    }

    suspend fun toggleMyList(key: String): Boolean {
        var nowIn = false
        context.dataStore.edit { prefs ->
            val current = prefs[keyMyList]?.toMutableSet() ?: mutableSetOf()
            if (key in current) {
                current.remove(key)
                nowIn = false
            } else {
                current.add(key)
                nowIn = true
            }
            prefs[keyMyList] = current
        }
        return nowIn
    }

    suspend fun myListKeys(): Set<String> =
        context.dataStore.data.first()[keyMyList] ?: emptySet()

    companion object {
        fun listKey(mediaType: String, id: Int): String = "$mediaType:$id"
    }
}
