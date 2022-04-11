package com.andrzejn.chainrelations.logic

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Values that do not change during the game (except for the screen resize)
 */
class WorldConstants(
    /**
     * Total number of balls on the field
     */
    var ballsCount: Int
) {
    /**
     * Font height for the score display
     */
    var fontHeight: Int = 0

    /**
     * Ball radius
     */
    var radius: Float = 0f

    /**
     * Base line width (for connectors, sockets etc.)
     */
    var lineWidth: Float = 0f

    /**
     * The strong repulsion force applied near the screen bprders and when one ball overlaps with another
     */
    private var barrierForce: Float = 0f

    /**
     * Maximum normal repulsion force at the minimal distance (then it is reduced by squared distance)
     */
    private var baseRepulsion: Float = 0f

    /**
     * Resulting ball force clipping, to avoid too fastball moves
     */
    var forceLimit: Float = 0f

    /**
     * The ball radius squared, for the force calculations
     */
    private var radSquared: Float = 0f

    /**
     * Multiplier coefficient for sideDist calculation
     */
    private val distMult: Float = 1.2f

    /**
     * Width of the left/right screen side panels with buttons
     */
    private var sideDist: Float = 0f
    private var borderDist: Float = 0f
    var attraction: Float = 0f
    var width: Float = 1f
    var height: Float = 1f
    var buttonSize: Float = 0f

    fun setValues(w: Float, h: Float) {
        width = w
        height = h
        fontHeight = (h / 30).toInt()
        buttonSize = w / 20f
        radius = calcRadius()
        lineWidth = radius / 20f
        radSquared = radius * radius
        sideDist = radius * distMult
        borderDist = 4f * sideDist * sideDist
        barrierForce = 17f * radius
        baseRepulsion = 11f * radius * borderDist
        forceLimit = 0.8f * radius
        attraction = 6f * radius
    }

    private fun calcRadius(): Float {
        val minSide = min(width - 2 * buttonSize, height)
        val maxSide = max(width - 2 * buttonSize, height)
        var count = 1
        while (true) {
            val r = minSide / count
            val count2: Int = floor(maxSide / r).toInt()
            if (count * count2 >= ballsCount)
                return r / 2.6f // Empirical coefficient for optimal ball size
            count++
        }
    }

    fun repulsion(distSquared: Float): Float =
        if (distSquared <= borderDist) barrierForce else
            if (distSquared > 9 * radSquared) 0f else baseRepulsion / distSquared

    fun applyBorderForce(b: Ball) {
        with(b.force[0]) {
            if (b.coord.x < sideDist + buttonSize) x += barrierForce
            else if (b.coord.x > width - sideDist - buttonSize) x -= barrierForce
            if (b.coord.y < sideDist) y += barrierForce
            else if (b.coord.y > height - sideDist) y -= barrierForce
        }
    }

    val randomCoordHit: Float get() = Random.nextFloat() * 3 * radius - 1.5f * radius

}