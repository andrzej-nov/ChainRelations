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

/**
 * The game ball object. Moves and rotates according to the simplified physics rules, has color sockets
 * and blinking eyes.
 */
class Ball(
    /**
     * Reference to the main app context
     */
    val ctx: Context,
    /**
     * An unique number of this ball, for serialization/deserialization purposes
     */
    var number: Int
) {
    private var cnt = 1

    /**
     * Inbound sockets
     */
    val inSock: Array<InSocket> = Array(3) { InSocket(ctx, this, cnt++) }

    /**
     * Outbound sockets
     */
    val outSock: Array<OutSocket> = Array(3) { OutSocket(ctx, this, cnt++) }

    /**
     * All sockets, inbound+outbound, to simplify iterations
     */
    val sockets: List<BaseSocket> = inSock.toList<BaseSocket>().plus(outSock)

    /**
     * Maximum colors count (a copy of the game settings value, for slightly faster and cleaner access)
     */
    private val maxColor: Int = ctx.gs.colorsCount

    /**
     * All the linear forces affecting this ball.
     * force[0] is the border barrier force + accelerometers, then go up to 6 connectors attraction forces,
     * then repulsions from other balls
     */
    var force: Array<Vector2> = Array(ctx.wc.ballsCount + 7) { Vector2(0f, 0f) }

    /**
     * Current ball center coordinates
     */
    val coord: Vector2 = Vector2()

    /**
     * Angular force momentum
     */
    var torque: Float = 0f

    /**
     * Current ball rotation. Clipped to 0..2*PI
     */
    private var angle: Float = 0f

    /**
     * Internal force array counter for the forces calculation
     */
    private var i: Int = 0

    init {
        reset()
    }

    /**
     * Reset the ball angle and sockets (make it a new ball on the same position, used when the ball dies and
     * new one is born instead)
     */
    fun reset() {
        angle = 0f
        val color = (1..maxColor).plus((1..maxColor)).shuffled().iterator()
        sockets.forEach { it.color = color.next() }
    }

    /**
     * Change a random unconnected socket color. Invoked on ball blinking.
     */
    fun recolorRandomSocket() {
        sockets.filter { it.conn == null }.random().color = Random.nextInt(maxColor) + 1
    }

    /**
     * Set one more force to the forces array
     */
    fun addForce(f: Vector2): Vector2 = force[i++].set(f)

    /**
     * Clear all the forces before new calculation
     */
    fun clearForces() {
        force.forEach { it.set(0f, 0f) }
        torque = 0f
        i = 1
    }

    /**
     * Internal variable to reduce GC load on creating a lot of transient objects
     */
    private val f = Vector2()

    /**
     * Calculate and set all repulsion forces from other balls
     */
    fun calcRepulsions(balls: List<Ball>) {
        balls.forEach { that ->
            f.set(this.coord).sub(that.coord).setLength(ctx.wc.repulsion(this.coord.dst2(that.coord)))
            addForce(f)
            that.addForce(f.scl(-1f))
        }
    }

    /**
     * Apply slight accelerometer force, to let the balls respons to the real world gravitation
     */
    fun applyAccelerometers(): Vector2 =
        force[0].add(Gdx.input.accelerometerY * ctx.wc.radius / 5, -Gdx.input.accelerometerX * ctx.wc.radius / 5)

    /**
     * Visual ball center coordinates. May differ from the actual coord by 1 pixel, to avoid balls' minor visual
     * trembling.
     */
    val drawCoord: Vector2 = Vector2(-1f, -1f)

    /**
     * Move/rotate the ball according to the applied forced.
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

    /**
     * Visual ball rotation angle. May differ from the actual angle by 0.01 radians, to avoid balls' minor visual
     * trembling.
     */
    private var prevAngle: Float = 1f

    /**
     * Recalculate sockets and eyes positions. Usually skips when there is no significant angle changes,
     * but there is the override parameter to always recalculate, when the eyes size is tweened.
     */
    fun setElementCoords(acceptAnyAngleChangeAmount: Boolean = false) {
        if (!acceptAnyAngleChangeAmount && abs(angle - prevAngle) < 0.01)
            return
        prevAngle = angle
        var a = angle + PI.toFloat() / 6
        val r = ctx.wc.radius * 0.8f
        val markSide = ctx.wc.radius * 0.1f
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

    /**
     * Render sockets and eyes
     */
    fun drawDetails(k: Float = 1f, center: Vector2 = drawCoord) {
        sockets.forEach { it.draw(k, center, alpha) }
        drawEyes(k, center)
    }

    private var eyeL: Pair<Vector2, Vector2> = Vector2() to Vector2()
    private var eyeR: Pair<Vector2, Vector2> = Vector2() to Vector2()

    /**
     * The eyes height coefficient, for tweening.
     */
    var eyeK: Float = 1f

    /**
     * Is the ball in the blinking animation
     */
    var inBlink: Boolean = false

    /**
     * Is the ball in the death animation
     */
    var inDeath: Boolean = false

    /**
     * The ball alpha channel
     */
    var alpha: Float = 1f

    /**
     * Recalculate eyes angle/size
     */
    fun updateEyeCoords() {
        val len = ctx.wc.radius * 0.1f
        eyeL.first.set(-2 * len, -2 * len * eyeK).rotateRad(angle)
        eyeL.second.set(-2 * len, 3 * len * eyeK).rotateRad(angle)
        eyeR.first.set(2 * len, -2 * len * eyeK).rotateRad(angle)
        eyeR.second.set(2 * len, 3 * len * eyeK).rotateRad(angle)
    }

    /**
     * Internal calculations variables to reduce the GC load
     */
    private val eye = Vector2() to Vector2()
    private val c: Color = Color()

    /**
     * Apply alpha channel to the given color
     */
    private fun alphaColor(color: Color): Color = c.set(color).also { it.a = alpha }

    /**
     * Draw single eye
     */
    private fun drawEye(e: Pair<Vector2, Vector2>, k: Float, center: Vector2) {
        ctx.sd.line(
            eye.first.set(e.first).scl(k).add(center),
            eye.second.set(e.second).scl(k).add(center),
            ctx.wc.lineWidth * 3 * k
        )
    }

    /**
     * Draw eyes
     */
    private fun drawEyes(k: Float, center: Vector2) {
        ctx.sd.setColor(alphaColor(ctx.theme.eyeColor))
        drawEye(eyeL, k, center)
        drawEye(eyeR, k, center)
    }

    /**
     * Serialize the ball for save game
     */
    fun serialize(sb: StringBuilder) {
        sb.append(number, 2).append(coord.x.toInt(), 4).append(coord.y.toInt(), 4)
            .append((angle * 1000).toInt(), 4)
        sockets.forEach { it.serialize(sb) }
    }

    /**
     * Deserialize the ball for load game
     */
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