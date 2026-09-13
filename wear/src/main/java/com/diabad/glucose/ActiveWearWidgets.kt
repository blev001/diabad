package com.diabad.glucose

import android.content.Context
import androidx.wear.tiles.TileService
import java.util.concurrent.Executors

/**
 * Remembers which tiles and complications the user actually added, so a glucose
 * update does not wake every unused DiaBAD surface.
 */
object ActiveWearWidgets {
    private const val PREFS = "diabad_active_widgets"
    private const val KEY_TILES = "tiles"
    private const val KEY_TILES_TRACKED = "tiles_tracked"
    private const val KEY_COMPLICATIONS = "complications"
    private const val KEY_COMPLICATIONS_TRACKED = "complications_tracked"
    private val executor = Executors.newSingleThreadExecutor()

    fun markTile(context: Context, service: Class<*>, added: Boolean) {
        val prefs = prefs(context)
        val current = prefs.getStringSet(KEY_TILES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (added) current.add(service.name) else current.remove(service.name)
        prefs.edit()
            .putBoolean(KEY_TILES_TRACKED, true)
            .putStringSet(KEY_TILES, current)
            .apply()
    }

    fun markComplication(context: Context, service: Class<*>, instanceId: Int, added: Boolean) {
        val prefs = prefs(context)
        val token = "${service.name}#$instanceId"
        val current = prefs.getStringSet(KEY_COMPLICATIONS, emptySet())?.toMutableSet()
            ?: mutableSetOf()
        if (added) current.add(token) else current.remove(token)
        prefs.edit()
            .putBoolean(KEY_COMPLICATIONS_TRACKED, true)
            .putStringSet(KEY_COMPLICATIONS, current)
            .apply()
    }

    fun shouldUpdateTile(context: Context, service: Class<out TileService>): Boolean {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_TILES_TRACKED, false)) return true
        return service.name in prefs.getStringSet(KEY_TILES, emptySet()).orEmpty()
    }

    fun complicationUpdates(context: Context, all: Collection<Class<*>>): List<Pair<Class<*>, IntArray>> {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_COMPLICATIONS_TRACKED, false)) {
            return all.map { it to intArrayOf() }
        }
        val tokens = prefs.getStringSet(KEY_COMPLICATIONS, emptySet()).orEmpty()
        return all.mapNotNull { clazz ->
            val ids = tokens.mapNotNull { token ->
                val parts = token.split('#')
                if (parts.size == 2 && parts[0] == clazz.name) parts[1].toIntOrNull() else null
            }
            if (ids.isEmpty()) null else clazz to ids.toIntArray()
        }
    }

    fun refreshTilesFromSystem(context: Context) {
        val app = context.applicationContext
        val future = TileService.getActiveTilesAsync(app, executor)
        future.addListener({
            runCatching {
                val names = future.get().map { it.componentName.className }.toSet()
                prefs(app).edit()
                    .putBoolean(KEY_TILES_TRACKED, true)
                    .putStringSet(KEY_TILES, names)
                    .apply()
            }
        }, executor)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
