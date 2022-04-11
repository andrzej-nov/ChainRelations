package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder

/**
 * The connector between two sockets
 */
class Connector(
    /**
     * Incoming socket
     */
    val inSocket: InSocket,
    /**
     * Outgoing socket
     */
    val outSocket: OutSocket,
    /**
     * A copy of the attraction force value, to avoid references to the WorldConstants and speed up the calculations
     * a little
     */
    var attraction: Float
) {
    init {
        inSocket.conn = this
        outSocket.conn = this
    }

    /**
     * Connector color. Assuming the in and out sockets have matching colors (that is not checked here).
     * The color never changes for the connector lifetime.
     */
    var color: Int = inSocket.color

    /**
     * Internal calculations variable to reduce the GC load
     */
    private val f = Vector2()

    /**
     * Calculate and apply the sockets attraction force.
     * Simplified physics: connectors are not elastic, the attracton force is simple constant.
     */
    fun calcAttraction() {
        f.set(outSocket.absCoord()).sub(inSocket.absCoord()).setLength(attraction)
        inSocket.addForce(f)
        outSocket.addForce(f.scl(-1f))
    }

    /**
     * Cleanup the connector on deletion
     */
    fun clear() {
        inSocket.conn = null
        outSocket.conn = null
    }

    /**
     * Serialize the connector for save game (it is deserialized in the Ball's method)
     */
    fun serialize(sb: StringBuilder) {
        sb.append(color).append(inSocket.ball.number, 3).append(inSocket.number)
            .append(outSocket.ball.number, 3).append(outSocket.number)
    }
}