package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import kotlin.math.PI

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

    /**
     * Simplified physics: movement is proportional to the force (consider it a movement in a viscous medium)
     */
    fun moveBy(delta: Float, calcSteps: Int) {
        coord.add(
            force.fold(f.set(Vector2.Zero)) { v, frc -> v.mulAdd(frc, delta) }.clamp(0f, ctx.wc.forceLimit)
                .scl(1f / calcSteps)
        )
        if (torque != 0f) {
            angle += torque * delta / (ctx.wc.radius * 50 * calcSteps) // 50 is empirical constant for smooth rotation
            setSocketCoords()
        }
    }

    fun setSocketCoords() {
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
    }

    fun drawSockets(k: Float = 1f, center: Vector2 = coord) {
        sockets.forEach { it.draw(k, center) }
        drawEyes(k, center)
    }

    var eyeH: Float = 0.5f
    var eyeW: Float = 0.1f
    var eye1 = Vector2(-ctx.wc.radius * 0.3f, -ctx.wc.radius * 0.2f)
    var eye2 = Vector2(ctx.wc.radius * 0.2f, -ctx.wc.radius * 0.2f)

    fun drawEyes(k: Float = 1f, center: Vector2 = coord) {
        ctx.sd.setColor(Color.LIGHT_GRAY)
        f.set(eye1).rotateRad(angle).add(coord)
        ctx.sd.filledRectangle(f.x, f.y, eyeW * ctx.wc.radius, eyeH * ctx.wc.radius, angle)
        f.set(eye2).rotateRad(angle).add(coord)
        ctx.sd.filledRectangle(f.x, f.y, eyeW * ctx.wc.radius, eyeH * ctx.wc.radius, angle)
    }

}