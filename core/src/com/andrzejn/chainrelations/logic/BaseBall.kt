package com.andrzejn.chainrelations.logic

class BaseBall {
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

    fun setValues(r: Float) {
        radius = r
        lineWidth = r / 20f
        radSquared = r * r
        sideDist = r * distMult
        borderDist = 4f * sideDist * sideDist
        barrierForce = 15f * r
        baseRepulsion = 9f * r * borderDist
        forceLimit = 0.8f * r
        attraction = 6f * r
    }

    fun repulsion(distSquared: Float): Float =
        if (distSquared <= borderDist) barrierForce else
            if (distSquared > 9 * radSquared) 0f else baseRepulsion / distSquared

    fun applyBorderForce(width: Float, height: Float, b: Ball) {
        with(b.force[0]) {
            if (b.coord.x < sideDist) x += barrierForce
            else if (b.coord.x > width - sideDist) x -= barrierForce
            if (b.coord.y < sideDist) y += barrierForce
            else if (b.coord.y > height - sideDist) y -= barrierForce
        }
    }

}