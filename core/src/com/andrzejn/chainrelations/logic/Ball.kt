package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import space.earlygrey.shapedrawer.JoinType
import space.earlygrey.shapedrawer.ShapeDrawer
import kotlin.math.PI

class Ball(val ballsCount: Int, val wc: WorldConstants) {
    val inCom: Array<Socket> = Array(3) { Socket(this, true) }
    val outCom: Array<Socket> = Array(3) { Socket(this, false) }

    init {
        reset()
    }

    // force[0] is border barrier force, then go connectors attraction, then repulsions from other balls
    val force = Array(ballsCount + 7) { Vector2(0f, 0f) }
    val coord: Vector2 = Vector2()

    var torque = 0f
    var angle: Float = 0f

    var i = 0 // Array counter for the forces calculation

    fun reset() {
        angle = 0f
        val color = (1..7).plus((1..7)).shuffled().iterator()
        inCom.plus(outCom).forEach { it.color = color.next() }
    }

    fun setSocketCoords() {
        var a = angle + PI.toFloat() / 6
        val r = wc.radius * 0.8f
        var markSide = wc.radius * 0.1f
        outCom.forEach {
            it.coord.set(r, 0f)
            it.mark[0].set(it.coord).add(markSide, 0f)
            it.mark[1].set(it.coord).add(-markSide, -markSide)
            it.mark[2].set(it.coord).add(-markSide, markSide)
            it.mark.plus(it.coord).forEach { v -> v.rotateRad(a) }
            a += PI.toFloat() / 3f
        }
        inCom.forEach {
            it.coord.set(r, 0f)
            it.mark[0].set(it.coord).add(-markSide, -markSide)
            it.mark[1].set(it.coord).add(-markSide, markSide)
            it.mark[2].set(it.coord).add(markSide, markSide)
            it.mark[3].set(it.coord).add(markSide, -markSide)
            it.mark.plus(it.coord).forEach { v -> v.rotateRad(a) }
            a += PI.toFloat() / 3f
        }
    }

    fun draw(sd: ShapeDrawer, k: Float, c: Array<Color>, center: Vector2 = coord) {
        val lw = wc.lineWidth * k
        val rad = wc.radius * 0.15f * k
        outCom.plus(inCom).forEach {
            sd.setColor(c[it.color])
            if (it.conn != null)
                sd.filledCircle(Vector2(it.coord).scl(k).add(center), rad)
            else if (it.isInCom)
                sd.polygon(
                    arrayOf(
                        it.mark[0].x * k + center.x,
                        it.mark[0].y * k + center.y,
                        it.mark[1].x * k + center.x,
                        it.mark[1].y * k + center.y,
                        it.mark[2].x * k + center.x,
                        it.mark[2].y * k + center.y,
                        it.mark[3].x * k + center.x,
                        it.mark[3].y * k + center.y
                    ).toFloatArray(), lw, JoinType.SMOOTH
                )
            else sd.triangle(
                Vector2(it.mark[0]).scl(k).add(center),
                Vector2(it.mark[1]).scl(k).add(center),
                Vector2(it.mark[2]).scl(k).add(center),
                lw
            )
        }
    }

    fun clearForces() {
        force.forEach { it.set(0f, 0f) }
        torque = 0f
        i = 1
    }

    fun calcRepulsions(balls: List<Ball>) {
        balls.forEach {
            val f = Vector2(coord).sub(it.coord).setLength(wc.repulsion(coord.dst2(it.coord)))
            addForce(f)
            it.addForce(f.scl(-1f))
        }
    }

    fun addForce(f: Vector2) = force[i++].set(f)

    fun applyBorderForce(width: Float, height: Float) = wc.applyBorderForce(width, height, this)

    /**
     * Simplified physics: movement is proportional to the force (consider it a movement in a viscous medium)
     */
    fun moveBy(delta: Float, step: Float) {
        coord.add(force.fold(Vector2()) { v, f -> v.mulAdd(f, delta) }.clamp(0f, wc.forceLimit).scl(step))
        if (torque != 0f) {
            angle += torque * delta / (wc.radius * 1000f)
            setSocketCoords()
        }
    }

}