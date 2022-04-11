package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.math.Vector2

/**
 * Outgoing socket. Rendered as triangle, there is no orhr specific logic.
 */
class OutSocket(ctx: Context, ball: Ball, number: Int) : BaseSocket(ctx, ball, number) {
    /**
     * Visual socket polygon coordinates for rendering (relative to the ball center)
     */
    override val mark: Array<Vector2> = Array(3) { Vector2() }

    /**
     * Assign the connector coordinates for the given size and rotation angle
     */
    fun setup(r: Float, len: Float, a: Float) {
        super.setup(r)
        mark[0].set(coord).add(len, 0f)
        mark[1].set(coord).add(-len, -len)
        mark[2].set(coord).add(-len, len)
        mark.plus(coord).forEach { it.rotateRad(a) }
    }

    /**
     * Internal calculations variable to reduce the GC load
     */
    private val v = Array(3) { Vector2() }

    /**
     * Render the socket. This implementation renders the unconnected socket case
     */
    override fun draw(k: Float, center: Vector2, alpha: Float) {
        super.draw(k, center, alpha)
        if (conn != null)
            return
        ctx.sd.triangle(
            v[0].set(mark[0]).scl(k).add(center),
            v[1].set(mark[1]).scl(k).add(center),
            v[2].set(mark[2]).scl(k).add(center),
            ctx.wc.lineWidth * k
        )
    }
}