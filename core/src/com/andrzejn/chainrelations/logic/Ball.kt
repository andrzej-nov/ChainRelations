package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import space.earlygrey.shapedrawer.ShapeDrawer
import kotlin.math.PI

class Ball(val ballsCount: Int, val bb: BaseBall) {
    val inCom: Array<Socket> = Array(3) { Socket(this) }
    val outCom: Array<Socket> = Array(3) { Socket(this) }

    // force[0] is border barrier force, then go connectors attraction, then repulsions from other balls
    val force = Array(ballsCount + 7) { Vector2(0f, 0f) }
    val coord: Vector2 = Vector2()

    var torque = 0f
    var angle: Float = 0f

    var i = 0 // Array counter for the forces calculation

    fun setSocketCoords() {
        var a = angle + PI.toFloat() / 6
        val r = bb.radius * 0.8f
        var triL = bb.radius * 0.1f
        outCom.forEach {
            it.coord.set(r, 0f)
            it.tri[0].set(it.coord).add(triL, 0f)
            it.tri[1].set(it.coord).add(-triL, -triL)
            it.tri[2].set(it.coord).add(-triL, triL)
            it.tri.plus(it.coord).forEach { v -> v.rotateRad(a) }
            a += PI.toFloat() / 3f
        }
        inCom.forEach {
            it.coord.set(r, 0f)
            it.tri[0].set(it.coord).add(-triL, 0f)
            it.tri[1].set(it.coord).add(triL, -triL)
            it.tri[2].set(it.coord).add(triL, triL)
            it.tri.plus(it.coord).forEach { v -> v.rotateRad(a) }
            a += PI.toFloat() / 3f
        }
    }

    fun draw(sd: ShapeDrawer, k: Float, c: Array<Color>) {
        val lw = bb.lineWidth * k
        val rad = k * bb.radius * 0.15f
        outCom.plus(inCom).forEach {
            sd.setColor(c[it.color])
            if (it.conn != null)
                sd.filledCircle(Vector2(it.coord).scl(k).add(this.coord), rad)
            else sd.triangle(
                Vector2(it.tri[0]).scl(k).add(this.coord),
                Vector2(it.tri[1]).scl(k).add(this.coord),
                Vector2(it.tri[2]).scl(k).add(this.coord),
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
            val f = Vector2(coord).sub(it.coord).setLength(bb.repulsion(coord.dst2(it.coord)))
            addForce(f)
            it.addForce(f.scl(-1f))
        }
    }

    fun addForce(f: Vector2) = force[i++].set(f)

    fun applyBorderForce(width: Float, height: Float) = bb.applyBorderForce(width, height, this)

    /**
     * Simplified physics: movement is proportional to the force (consider it a movement in a viscous medium)
     */
    fun moveBy(delta: Float, step: Float) {
        coord.add(force.fold(Vector2()) { v, f -> v.mulAdd(f, delta) }.clamp(0f, bb.forceLimit).scl(step))
        if (torque != 0f) {
            angle += torque * delta / (bb.radius * 1000f)
            setSocketCoords()
        }
    }

}