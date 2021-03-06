package com.andrzejn.chainrelations.helper

import com.badlogic.gdx.Gdx
import java.util.*

/**
 * Game settings and saved game. Stored in the GDX system-dependent Preferences
 */
class GameSettings {
    private val pref by lazy { Gdx.app.getPreferences("com.andrzejn.chainrelations") }
    private val sMAXRADIUS = "maxRadius"
    private val sBALLSCOUNT = "ballsCount"
    private val sCOLORSCOUNT = "colorsCount"
    private val sISRECYCLE = "isRecycle"
    private val sSAVEDGAME = "savedGame"
    private val sDARKTHEME = "darkTheme"
    private val sINGAMEDURATION = "inGameDuration"
    private val sRECORDMOVES = "recordMoves"
    private val sRECORDPOINTS = "recordPoints"
    private var iMaxRadius: Float = 4.5f
    private var iBallsCount: Int = 20
    private var iColorsCount: Int = 6
    private var iIsRecycle: Boolean = true
    private var iDarkTheme: Boolean = true
    private var iInGameDuration: Long = 0

    /**
     * Reset game settings to default values
     */
    fun reset() {
        iMaxRadius = pref.getFloat(sMAXRADIUS, 4.5f)
        iMaxRadius = iMaxRadius.coerceIn(3f, 6f)
        maxRadius = iMaxRadius
        iBallsCount = pref.getInteger(sBALLSCOUNT, 20)
        iBallsCount = iBallsCount.coerceIn(20, 60)
        ballsCount = iBallsCount
        iColorsCount = pref.getInteger(sCOLORSCOUNT, 6)
        iColorsCount = iColorsCount.coerceIn(6, 7)
        colorsCount = iColorsCount
        iIsRecycle = pref.getBoolean(sISRECYCLE, true)
        isRecycle = iIsRecycle
        iDarkTheme = pref.getBoolean(sDARKTHEME, true)
        isDarkTheme = iDarkTheme
        iInGameDuration = pref.getLong(sINGAMEDURATION, 0)
    }

    /**
     * Maximum connector radius, 3..6
     */
    var maxRadius: Float
        get() = iMaxRadius
        set(value) {
            iMaxRadius = value
            pref.putFloat(sMAXRADIUS, value)
            pref.flush()
        }

    /**
     * Balls count, 20..60
     */
    var ballsCount: Int
        get() = iBallsCount
        set(value) {
            iBallsCount = value
            pref.putInteger(sBALLSCOUNT, value)
            pref.flush()
        }

    /**
     * Number of different colors used for ball sockets. 6..7
     */
    var colorsCount: Int
        get() = iColorsCount
        set(value) {
            iColorsCount = value
            pref.putInteger(sCOLORSCOUNT, value)
            pref.flush()
        }

    /**
     * Are dead balls respawn or die permanently
     */
    var isRecycle: Boolean
        get() = iIsRecycle
        set(value) {
            iIsRecycle = value
            pref.putBoolean(sISRECYCLE, value)
            pref.flush()
        }

    /**
     * Dark/Light color theme selector
     */
    var isDarkTheme: Boolean
        get() = iDarkTheme
        set(value) {
            iDarkTheme = value
            pref.putBoolean(sDARKTHEME, value)
            pref.flush()
        }

    /**
     * Total time spent in game
     */
    var inGameDuration: Long
        get() = iInGameDuration
        set(value) {
            iInGameDuration = value
            pref.putLong(sINGAMEDURATION, value)
            pref.flush()
        }

    /**
     * Serialized save game
     */
    var savedGame: String
        get() = pref.getString(sSAVEDGAME, "")
        set(value) {
            pref.putString(sSAVEDGAME, value)
            pref.flush()
        }

    /**
     * Key name for storing the records for the current tile type - game size - colors
     */
    private fun keyName(prefix: String): String {
        return "$prefix$iBallsCount$iColorsCount${serializeFloat(iMaxRadius)}$isRecycle"
    }

    /***
     * Stable float serialization to the N.N format
     */
    private fun serializeFloat(f: Float): String = String.format(Locale.ROOT, "%.1f", f)

    /**
     * Record moves value for the current balls count - max radius - colors
     */
    var recordMoves: Int
        get() = pref.getInteger(keyName(sRECORDMOVES), 0)
        set(value) {
            pref.putInteger(keyName(sRECORDMOVES), value)
            pref.flush()
        }

    /**
     * Record moves value for the current balls count - max radius - colors
     */
    var recordPoints: Int
        get() = pref.getInteger(keyName(sRECORDPOINTS), 0)
        set(value) {
            pref.putInteger(keyName(sRECORDPOINTS), value)
            pref.flush()
        }

    /**
     * Serialize game settings, to include into the saved game. Always 6 characters.
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(serializeFloat(iMaxRadius)).append(ballsCount).append(colorsCount).append(if (isRecycle) 1 else 0)
    }

    /**
     * Deserialize game settings from the saved game
     */
    fun deserialize(s: String): Boolean {
        if (s.length != 7) {
            reset()
            return false
        }
        val mr = s.substring(0..2).toFloatOrNull()
        val bc = s.substring(3..4).toIntOrNull()
        val cc = s[5].digitToIntOrNull()
        val ir = s[6].digitToIntOrNull()
        if (mr == null || mr !in 3f..6f
            || bc == null || bc !in 20..60
            || cc == null || cc !in 6..7
            || ir == null || ir !in 0..1
        ) {
            reset()
            return false
        }
        maxRadius = mr
        ballsCount = bc
        colorsCount = cc
        isRecycle = ir != 0
        return true
    }
}
