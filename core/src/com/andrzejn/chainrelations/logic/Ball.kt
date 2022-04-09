package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import kotlin.math.PI
import kotlin.math.abs

class Ball(val ctx: Context) {
    val inSock: Array<InSocket> = Array(3) { InSocket(ctx, this) }
    val outSock: Array<OutSocket> = Array(3) { OutSocket(ctx, this) }
    val sockets: List<BaseSocket> = inSock.toList<BaseSocket>().plus(outSock)

    val maxColor: Int = 7 // In range 6..7. 5 is too easy

    // force[0] is border barrier force, then go connectors attraction, then repulsions from other balls
    val force = Array(ctx.wc.ballsCount + 7) { Vector2(0f, 0f) }
    val coord: Vector2 = Vector2()

    var torque = 0f
    var angle: Float = 0f

    var i = 0 // Array counter for the forces calculation

    init {
        reset()
    }

    fun reset() {
        angle = 0f
        val color = (1..maxColor).plus((1..maxColor)).shuffled().iterator()
        sockets.forEach { it.color = color.next() }
    }

    fun addForce(f: Vector2) = force[i++].set(f)

    fun clearForces() {
        force.forEach { it.set(0f, 0f) }
        torque = 0f
        i = 1
    }

    private val f = Vector2()

    fun calcRepulsions(balls: List<Ball>) {
        balls.forEach { that ->
            f.set(this.coord).sub(that.coord).setLength(ctx.wc.repulsion(this.coord.dst2(that.coord)))
            addForce(f)
            that.addForce(f.scl(-1f))
        }
    }

    val drawCoord = Vector2(-1f, -1f)

    /**
     * Simplified physics: movement is proportional to the force (consider it a movement in a viscous medium)
     */
    fun moveBy(delta: Float, calcSteps: Int) {
        coord.add(
            force.fold(f.set(Vector2.Zero)) { v, frc -> v.mulAdd(frc, delta) }.clamp(0f, ctx.wc.forceLimit)
                .scl(1f / calcSteps)
        )
        if (drawCoord.dst(coord) > 1)
            drawCoord.set(coord)
        if (torque != 0f) {
            angle += torque * delta / (ctx.wc.radius * 50 * calcSteps) // 50 is empirical constant for smooth rotation
            setElementCoords()
        }
    }

    var prevAngle = 1f

    fun setElementCoords(forceUpdate: Boolean = false) {
        if (!forceUpdate && abs(angle - prevAngle) < 0.01)
            return
        prevAngle = angle
        var a = angle + PI.toFloat() / 6
        val r = ctx.wc.radius * 0.8f
        var markSide = ctx.wc.radius * 0.1f
        outSock.forEach {
            it.setup(r, markSide, a)
            a += PI.toFloat() / 3f
        }
        inSock.forEach {
            it.setup(r, markSide, a)
            a += PI.toFloat() / 3f
        }
        setEyeCoords()
    }

    fun drawElements(k: Float = 1f, center: Vector2 = drawCoord) {
        sockets.forEach { it.draw(k, center) }
        drawEyes(k, center)
    }

    var eyeL = Vector2() to Vector2()
    var eyeR = Vector2() to Vector2()

    fun setEyeCoords() {
        val len = ctx.wc.radius * 0.1f
        eyeL.first.set(-2 * len, -2 * len).rotateRad(angle)
        eyeL.second.set(-2 * len, 3 * len).rotateRad(angle)
        eyeR.first.set(2 * len, -2 * len).rotateRad(angle)
        eyeR.second.set(2 * len, 3 * len).rotateRad(angle)
    }

    private val eye = Vector2() to Vector2()

    fun drawEyes(k: Float, center: Vector2) {
        ctx.sd.setColor(Color.GRAY)
        ctx.sd.line(
            eye.first.set(eyeL.first).scl(k).add(center),
            eye.second.set(eyeL.second).scl(k).add(center),
            ctx.wc.lineWidth * 3 * k
        )
        ctx.sd.line(
            eye.first.set(eyeR.first).scl(k).add(center),
            eye.second.set(eyeR.second).scl(k).add(center),
            ctx.wc.lineWidth * 3 * k
        )
    }

}