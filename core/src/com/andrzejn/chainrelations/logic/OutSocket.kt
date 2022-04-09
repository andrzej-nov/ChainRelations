package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.math.Vector2

class OutSocket(ctx: Context, ball: Ball) : BaseSocket(ctx, ball) {
    override val mark = Array(3) { Vector2() }

    fun setup(r: Float, len: Float, a: Float) {
        super.setup(r)
        mark[0].set(coord).add(len, 0f)
        mark[1].set(coord).add(-len, -len)
        mark[2].set(coord).add(-len, len)
        mark.plus(coord).forEach { it.rotateRad(a) }
    }

    private val v = Array(3) { Vector2() }

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