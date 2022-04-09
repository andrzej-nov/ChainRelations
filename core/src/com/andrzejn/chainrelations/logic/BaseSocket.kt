package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2

abstract class BaseSocket(val ctx: Context, val ball: Ball) {
    var color: Int = 0
    val coord = Vector2()
    abstract val mark: Array<Vector2>
    var conn: Connector? = null

    fun addForce(f: Vector2) {
        ball.torque += coord.crs(f)
        ball.addForce(f)
    }

    protected fun setup(r: Float) = coord.set(r, 0f)

    private val v = Vector2()

    fun absCoord(): Vector2 = v.set(coord).add(ball.coord)

    fun absDrawCoord(): Vector2 = v.set(coord).add(ball.drawCoord)

    val c = Color()
    fun alphaColor(color: Color, alpha: Float): Color = c.set(color).also { it.a = alpha }

    open fun draw(k: Float, center: Vector2, alpha: Float) {
        ctx.sd.setColor(alphaColor(ctx.light[color], alpha))
        if (conn != null)
            ctx.sd.filledCircle(v.set(coord).scl(k).add(center), ctx.wc.radius * 0.15f * k)
    }
}