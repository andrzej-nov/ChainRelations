package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder

/**
 * The ball socket. Base object provides the logic common for incoming and outgoing sockets
 */
abstract class BaseSocket(
    /**
     * Reference to the main app context
     */
    val ctx: Context,
    /**
     * The ball that has this socket
     */
    val ball: Ball,
    /**
     * An unique socket number within the ball, for sereializaton/deserialization purposes
     */
    var number: Int
) {
    /**
     * Socket center coordinate, relative to the ball center.
     */
    val coord: Vector2 = Vector2()

    /**
     * Socket color, 1..maxColor. There are no uncolored sockets on valid balls/
     */
    var color: Int = 0

    /**
     * Visual socket polygon coordinates for rendering (relative to the ball center)
     */
    abstract val mark: Array<Vector2>

    /**
     * The connector plugged into this socket, if any
     */
    var conn: Connector? = null

    /**
     * Apply the force to the ball via this socket
     */
    fun addForce(f: Vector2) {
        ball.torque += coord.crs(f)
        ball.addForce(f)
    }

    /**
     * Reset the connector coordinates to initial default
     */
    protected fun setup(r: Float): Vector2 = coord.set(r, 0f)

    /**
     * Internal calculations variable to reduce the GC load
     */
    private val v = Vector2()

    /**
     * Absolute physical world coords of this socket
     */
    fun absCoord(): Vector2 = v.set(coord).add(ball.coord)

    /**
     * Absolute screen coords of this socket (as the ball may be drawn slightly not at its precise physical position)
     */
    fun absDrawCoord(): Vector2 = v.set(coord).add(ball.drawCoord)

    /**
     * Socket color
     */
    private val c: Color = Color()

    /**
     * Apply the alha channel to the given color
     */
    private fun alphaColor(color: Color, alpha: Float): Color = c.set(color).also { it.a = alpha }

    /**
     * Render the socket. The base implementation renders the connected socket case
     */
    open fun draw(k: Float, center: Vector2, alpha: Float) {
        ctx.sd.setColor(alphaColor(ctx.theme.light[color], alpha))
        if (conn != null)
            ctx.sd.filledCircle(v.set(coord).scl(k).add(center), ctx.wc.radius * 0.15f * k)
    }

    /**
     * Serialize the socket for the save game
     */
    fun serialize(sb: StringBuilder) {
        sb.append(number).append(color)
    }

    /**
     * Deserialize the socket for the load game
     */
    fun deserialize(s: String, i: Int): Int {
        number = s[i].digitToInt()
        color = s[i + 1].digitToInt()
        return i + 2
    }
}