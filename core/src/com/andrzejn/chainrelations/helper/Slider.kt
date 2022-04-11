package com.andrzejn.chainrelations.helper

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.utils.Align
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

class Slider(
    val ctx: Context, val minValue: Float, val maxValue: Float, var value: Float,
    /**
     * Precision = 1 means round value to whole integers, 0.1 means round to first decimal digit etc.
     */
    val precision: Float
) {
    var width: Float = 0f
    var height: Float = 0f
    var x: Float = 0f
    var y: Float = 0f
    var sliderX: Float = 0f
    var fontHeight: Int = 0
    val precisionFormat = if (precision < 1) "%.1f" else "%.0f"
    private lateinit var font: BitmapFont
    private lateinit var fc: BitmapFontCache

    init {
        normalizeValue(value)
    }

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

    fun normalizeValue(v: Float) {
        value = (round(v / precision) * precision).coerceIn(minValue, maxValue)
        sliderX = x + width * (value - minValue) / (maxValue - minValue)
        setText()
    }

    /**
     * Move slider. x is relative to the control box.
     */
    fun touch(xTouch: Float) {
        if (abs(xTouch - sliderX) < 2) return
        sliderX = xTouch
        normalizeValue(minValue + (xTouch - x) / width * (maxValue - minValue))
    }

    fun setText() {
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

    fun render() {
        ctx.sd.filledRectangle(x, y + height * 0.1f, width, height * 0.1f, ctx.theme.settingItem)
        ctx.sd.filledRectangle(sliderX, y, 10f, height * 0.3f, ctx.theme.settingSelection)
        fc.draw(ctx.sd.batch)
    }
}