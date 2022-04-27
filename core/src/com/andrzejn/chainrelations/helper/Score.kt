package com.andrzejn.chainrelations.helper

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.utils.Align

/**
 * Maintains current game score and draws it
 */
class Score(
    /**
     * Reference to the parent Context object
     */
    val ctx: Context
) {
    /**
     * Number of moves
     */
    private var moves: Int = 0

    /**
     * Number of points for killed balls
     */
    private var points: Int = 0

    private var recordMoves: Int = 0
    private var recordPoints: Int = 0

    // Font and text drawing objects
    private lateinit var font: BitmapFont
    private lateinit var fcMoves: BitmapFontCache
    private lateinit var fcPoints: BitmapFontCache

    private var textY: Float = 0f
    private var textMovesX: Float = 0f
    private var textPointsX: Float = 0f
    private var textWidth: Float = 0f
    private var fontHeight: Int = 0

    /**
     * Serialize the current score for the save game
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(moves, 5).append(points, 5)
    }

    /**
     * Deserialize and set the current score from saved game
     */
    fun deserialize(s: String): Boolean {
        if (s.length != 10) {
            reset()
            return false
        }
        val m = s.substring(0..4).toIntOrNull()
        val p = s.substring(5..9).toIntOrNull()
        if (m == null || p == null) {
            reset()
            return false
        }
        moves = m
        points = p
        // By this moment the game settings have been deserialized already, so we may rely on them
        loadRecords()
        return true
    }

    /**
     * Load record values for the current game settings
     */
    private fun loadRecords() {
        recordMoves = ctx.gs.recordMoves
        recordPoints = ctx.gs.recordPoints
    }

    /**
     * Reset score to zero
     */
    fun reset() {
        saveRecords()
        moves = 0
        points = 0
        loadRecords()
        if (fontHeight > 0)
            setTexts()
    }

    /**
     * Set font size and text positions for the current window size
     */
    fun setCoords(fontHeight: Int) {
        if (this.fontHeight != fontHeight) {
            if (this::font.isInitialized) // Check if lateinit property has been initialized
                font.dispose()
            font = ctx.createFont(fontHeight)
            this.fontHeight = fontHeight
            fcPoints = BitmapFontCache(font)
            fcMoves = BitmapFontCache(font)
        }
        textY = fontHeight.toFloat()
        textMovesX = 5f
        textWidth = 5f * fontHeight
        textPointsX = ctx.camera.viewportWidth - textWidth - 5f
        setTexts()
    }

    /**
     * Increment current moves counter
     */
    fun incrementMoves() {
        moves++
        setMovesText()
    }

    /**
     * Add score points
     */
    fun addPoints(points: Int) {
        this.points += points
        setPointsText()
    }

    /**
     * Update text objects with the current score values (then that text will be siply rendered as needed)
     */
    private fun setTexts() {
        setMovesText()
        setPointsText()
    }

    /**
     * Update text object with the current moves value (then that text will be siply rendered as needed)
     */
    private fun setMovesText() {
        fcMoves.setText(
            moves.toString() + if (moves > recordMoves) " !" else "",
            textMovesX,
            textY,
            textWidth,
            Align.left,
            false
        )
        fcMoves.setColors(ctx.theme.scoreMoves)
    }

    /**
     * Update text object with the current score value (then that text will be siply rendered as needed)
     */
    private fun setPointsText() {
        fcPoints.setText(
            points.toString() + if (points > recordPoints) " !" else "",
            textPointsX,
            textY,
            textWidth,
            Align.right,
            false
        )
        fcPoints.setColors(ctx.theme.scorePoints)
    }

    /**
     * Draw the scores in the provided batch
     */
    fun draw(batch: PolygonSpriteBatch) {
        fcMoves.draw(batch)
        fcPoints.draw(batch)
    }

    /**
     * Clean up the font object
     */
    fun dispose() {
        if (this::font.isInitialized)
            font.dispose()
    }

    /**
     * Update current records to the settings storage, as needed
     */
    fun saveRecords() {
        if (moves > recordMoves) {
            ctx.gs.recordMoves = moves
            recordMoves = moves
        }
        if (points > recordPoints) {
            ctx.gs.recordPoints = points
            recordPoints = points
        }
    }
}