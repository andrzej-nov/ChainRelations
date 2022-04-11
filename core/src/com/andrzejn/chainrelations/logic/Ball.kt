package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

class Ball(val ctx: Context, var number: Int) {
    private var cnt = 1
    val inSock: Array<InSocket> = Array(3) { InSocket(ctx, this, cnt++) }
    val outSock: Array<OutSocket> = Array(3) { OutSocket(ctx, this, cnt++) }
    val sockets: List<BaseSocket> = inSock.toList<BaseSocket>().plus(outSock)

    val maxColor: Int = ctx.gs.colorsCount

    // force[0] is the border barrier force + accelerometers, then go up to 6 connectors attraction forces,
    // then repulsions from other balls
    var force = Array(ctx.wc.ballsCount + 7) { Vector2(0f, 0f) }
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

    fun recolorRandomSocket() {
        sockets.filter { it.conn == null }.random().color = Random.nextInt(maxColor) + 1
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

    fun applyAccelerometers() =
        force[0].add(Gdx.input.accelerometerY * ctx.wc.radius / 5, -Gdx.input.accelerometerX * ctx.wc.radius / 5)

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
            if (angle < 0) angle += (2 * PI * (floor(-angle / 2 / PI) + 1)).toFloat()
            if (angle > 2 * PI) angle -= (2 * PI * floor(angle / 2 / PI)).toFloat()
            setElementCoords()
        }
    }

    var prevAngle = 1f

    fun setElementCoords(acceptAnyAngleChangeAmount: Boolean = false) {
        if (!acceptAnyAngleChangeAmount && abs(angle - prevAngle) < 0.01)
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
        updateEyeCoords()
    }

    fun drawDetails(k: Float = 1f, center: Vector2 = drawCoord) {
        sockets.forEach { it.draw(k, center, alpha) }
        drawEyes(k, center)
    }

    var eyeL = Vector2() to Vector2()
    var eyeR = Vector2() to Vector2()
    var eyeK: Float = 1f
    var inBlink = false
    var inDeath = false
    var alpha: Float = 1f

    fun updateEyeCoords() {
        val len = ctx.wc.radius * 0.1f
        eyeL.first.set(-2 * len, -2 * len * eyeK).rotateRad(angle)
        eyeL.second.set(-2 * len, 3 * len * eyeK).rotateRad(angle)
        eyeR.first.set(2 * len, -2 * len * eyeK).rotateRad(angle)
        eyeR.second.set(2 * len, 3 * len * eyeK).rotateRad(angle)
    }

    private val eye = Vector2() to Vector2()

    val c = Color()
    fun alphaColor(color: Color): Color = c.set(color).also { it.a = alpha }

    private fun drawEye(e: Pair<Vector2, Vector2>, k: Float, center: Vector2) {
        ctx.sd.line(
            eye.first.set(e.first).scl(k).add(center),
            eye.second.set(e.second).scl(k).add(center),
            ctx.wc.lineWidth * 3 * k
        )
    }

    fun drawEyes(k: Float, center: Vector2) {
        ctx.sd.setColor(alphaColor(ctx.theme.eyeColor))
        drawEye(eyeL, k, center)
        drawEye(eyeR, k, center)
    }

    fun serialize(sb: StringBuilder) {
        sb.append(number, 2).append(coord.x.toInt(), 4).append(coord.y.toInt(), 4)
            .append((angle * 1000).toInt(), 4)
        sockets.forEach { it.serialize(sb) }
    }

    fun deserialize(s: String, i: Int): Int {
        number = s.substring(i..i + 1).toInt()
        coord.x = s.substring(i + 2..i + 5).toFloat()
        coord.y = s.substring(i + 6..i + 9).toFloat()
        angle = s.substring(i + 10..i + 13).toFloat() / 1000
        var j = i + 14
        sockets.forEach { j = it.deserialize(s, j) }
        clearForces()
        return j
    }

}