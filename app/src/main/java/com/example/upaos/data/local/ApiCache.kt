package com.example.upaos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.apiDataStore by preferencesDataStore("api_cache")

/**
 * Caché local de la última respuesta exitosa de los endpoints de la app
 * (asistencia, horario, etc.). Se guarda el JSON COMPLETO de la respuesta,
 * igual que GradesCache, para mostrar al instante lo último conocido
 * sin que parezca que está cargando.
 */
class ApiCache(private val context: Context,
    private val store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> = context.apiDataStore) {

    suspend fun guardar(clave: String, json: String) {
        val user = clave.split('_').firstOrNull { it.matches(Regex("[0-9]{9}")) }
        if (user != null && (AccountPrivacy.isDeleting(user) || !TokenManager(context).containsAccount(user))) return
        store.edit { prefs ->
            prefs[stringPreferencesKey(clave)] = json
            prefs[longPreferencesKey("time_$clave")] = System.currentTimeMillis()
        }
    }

    /** Atomic read/modify/write: concurrent notifications and read receipts cannot lose entries. */
    suspend fun actualizar(clave: String, transform: (String?) -> String): String? {
        var result: String? = null
        store.edit { prefs ->
            val user = clave.split('_').firstOrNull { it.matches(Regex("[0-9]{9}")) }
            if (user != null && (AccountPrivacy.isDeleting(user) || !TokenManager(context).containsAccount(user))) return@edit
            val key = stringPreferencesKey(clave)
            val previous = prefs[key]
            val next = transform(previous)
            if (next != previous) {
                prefs[key] = next
                prefs[longPreferencesKey("time_$clave")] = System.currentTimeMillis()
            }
            result = next
        }
        return result
    }

    suspend fun cargar(clave: String): String? {
        val prefs = store.data.first()
        return prefs[stringPreferencesKey(clave)]
    }

    suspend fun actualizado(clave: String): Long? = store.data.first()[longPreferencesKey("time_$clave")]
    fun observarFecha(clave: String) = store.data.map { it[longPreferencesKey("time_$clave")] }

    internal suspend fun clearAgenda(user: String) {
        val key = "agenda_${user.trim().lowercase()}"
        store.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
            prefs.remove(longPreferencesKey("time_$key"))
        }
    }

    suspend fun limpiar(usuario: String) {
        val user = usuario.trim().lowercase()
        store.edit { prefs ->
            prefs.asMap().keys.filter { key -> key.name.split('_').contains(user) }.forEach { prefs.remove(it) }
        }
    }

    /**
     * Devuelve los valores en caché (JSON) de las claves que empiecen
     * por [prefijo]. Útil para el widget de próxima clase, que no conoce
     * el periodo seleccionado y debe localizar cualquier horario guardado.
     */
    suspend fun listarPorPrefijo(prefijo: String): Map<String, String> {
        val prefs = store.data.first()
        return prefs.asMap()
            .filterKeys { it.name.startsWith(prefijo) }
            .mapKeys { it.key.name }
            .mapValues { it.value as? String ?: "" }
            .filterValues { it.isNotBlank() }
    }
}
