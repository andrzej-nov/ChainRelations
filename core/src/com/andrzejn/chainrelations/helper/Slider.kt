package com.andrzejn.chainrelations.helper

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.utils.Align
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

/**
 * The Slider UI control for the Home/Settings screen
 */
class Slider(
    /**
     * Reference to the main app context
     */
    val ctx: Context,
    /**
     * Minimum slider value
     */
    private val minValue: Float,
    /**
     * Maximum slider value
     */
    private val maxValue: Float,
    /**
     * Current slider value
     */
    var value: Float,
    /**
     * Precision = 1 means round value to whole integers, 0.1 means round to first decimal digit etc.
     */
    private val precision: Float
) {
    /**
     * Control screen width
     */
    var width: Float = 0f

    /**
     * Control screen height
     */
    var height: Float = 0f

    /**
     * Control left side on screen
     */
    var x: Float = 0f

    /**
     * Control bottom side on screen
     */
    var y: Float = 0f

    /**
     * The slider X coordinate (absolute screen one, not relative to the control box)
     */
    private var sliderX: Float = 0f
    private var fontHeight: Int = 0
    private val precisionFormat = if (precision < 1) "%.1f" else "%.0f"
    private lateinit var font: BitmapFont
    private lateinit var fc: BitmapFontCache

    init {
        normalizeValue(value)
    }

    /**
     * Position the control, calculate internal layout
     */
    fun setBounds(x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        val newFontHeight: Int = floor(height * 0.4f).toInt()
        if (newFontHeight == fontHeight) return
        if (this::font.isInitialized) // Check if lateinit property has been initialized
            font.dispose()
        font = ctx.createFont(newFontHeight)
        fc = BitmapFontCache(font)
        fontHeight = newFontHeight
        normalizeValue(value)
    }

    /**
     * Ensure the value is within range and given precision. Updates the slider position and text, too
     */
    fun normalizeValue(v: Float) {
        value = (round(v / precision) * precision).coerceIn(minValue, maxValue)
        sliderX = x + width * (value - minValue) / (maxValue - minValue)
        setText()
    }

    /**
     * Move slider. xTouch is absolute screen coordinate, not relative to the control box
     */
    fun touch(xTouch: Float) {
        if (abs(xTouch - sliderX) < 2) return
        sliderX = xTouch
        normalizeValue(minValue + (xTouch - x) / width * (maxValue - minValue))
    }

    /**
     * Update the value-above-slider text
     */
    private fun setText() {
        if (!this::fc.isInitialized) // Check if lateinit property has been initialized
            return
        fc.setText(
            String.format(precisionFormat, value),
            sliderX - width / 2,
            y + height * 0.6f,
            width,
            Align.top or Align.center,
            false
        )
        fc.setColors(ctx.theme.creditsText)
    }

    /**
     * Render the control
     */
    fun render() {
        ctx.sd.filledRectangle(x, y + height * 0.1f, width, height * 0.1f, ctx.theme.settingItem)
        ctx.sd.filledRectangle(sliderX, y, 10f, height * 0.3f, ctx.theme.settingSelection)
        fc.draw(ctx.sd.batch)
    }

    /**
     * Clean up
     */
    fun dispose() {
        if (this::font.isInitialized) // Check if lateinit property has been initialized
            font.dispose()
    }
}