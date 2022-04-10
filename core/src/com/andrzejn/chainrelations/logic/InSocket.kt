package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.math.Vector2
import space.earlygrey.shapedrawer.JoinType

class InSocket(ctx: Context, ball: Ball, number: Int) : BaseSocket(ctx, ball, number) {
    override val mark = Array(4) { Vector2() }

    fun setup(r: Float, len: Float, a: Float) {
        super.setup(r)
        mark[0].set(coord).add(-len, -len)
        mark[1].set(coord).add(-len, len)
        mark[2].set(coord).add(len, len)
        mark[3].set(coord).add(len, -len)
        mark.plus(coord).forEach { it.rotateRad(a) }
    }

    private val f = FloatArray(8)

    override fun draw(k: Float, center: Vector2, alpha: Float) {
        super.draw(k, center, alpha)
        if (conn != null)
            return
        f[0] = mark[0].x * k + center.x
        f[1] = mark[0].y * k + center.y
        f[2] = mark[1].x * k + center.x
        f[3] = mark[1].y * k + center.y
        f[4] = mark[2].x * k + center.x
        f[5] = mark[2].y * k + center.y
        f[6] = mark[3].x * k + center.x
        f[7] = mark[3].y * k + center.y
        ctx.sd.polygon(f, ctx.wc.lineWidth * k, JoinType.SMOOTH)
    }
}