package com.andrzejn.chainrelations.logic

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class WorldConstants(var ballsCount: Int) {
    var fontHeight: Int = 0
    val distMult = 1.2f
    var radius: Float = 0f
    var lineWidth = 0f
    var barrierForce: Float = 0f
    var baseRepulsion: Float = 0f
    var forceLimit: Float = 0f
    var radSquared: Float = 0f
    var sideDist: Float = 0f
    var borderDist: Float = 0f
    var attraction: Float = 0f
    var width: Float = 1f
    var height: Float = 1f
    var buttonSize: Float = 0f

    fun setValues(w: Float, h: Float, bCount: Int = ballsCount) {
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