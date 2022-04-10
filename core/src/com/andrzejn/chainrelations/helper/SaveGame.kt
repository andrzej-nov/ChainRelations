package com.andrzejn.chainrelations.helper

import com.andrzejn.chainrelations.Context
import com.andrzejn.chainrelations.logic.World

/**
 * Handles game save/load
 */
class SaveGame(
    /**
     * Reference to the parent Context object
     */
    val ctx: Context
) {

    /**
     * Serialize the whole game
     */
    private fun serialize(world: World): String {
        val sb = com.badlogic.gdx.utils.StringBuilder()
        ctx.gs.serialize(sb)
        ctx.score.serialize(sb)
        world.serialize(sb)
        return sb.toString()
    }

    private fun deserializeSettingsAndScore(s: String): Boolean {
        if (!ctx.gs.deserialize(s.substring(0..5))) return false
        if (!ctx.score.deserialize(s.substring(6..15))) return false
        return true
    }

    /**
     * Save current game to Preferences
     */
    fun saveGame(world: World) {
        ctx.gs.savedGame = serialize(world)
    }

    /**
     * Deletes saved game
     */
    fun clearSavedGame() {
        ctx.gs.savedGame = ""
    }

    /**
     * Serialized save game
     */
    fun savedGame(): String = ctx.gs.savedGame

    /**
     * Deserialize and set the game settings and score from the saved game
     */
    fun loadSettingsAndScore(s: String): Boolean {
        if (s.length < 31)
            return false
        return deserializeSettingsAndScore(s)
    }
}