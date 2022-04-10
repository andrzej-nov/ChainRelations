package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder

class Connector {
    val inSocket: InSocket
    val outSocket: OutSocket
    var attraction: Float

    constructor(inSocket: InSocket, outSocket: OutSocket, attraction: Float) {
        this.inSocket = inSocket
        this.outSocket = outSocket
        this.attraction = attraction
        this.color = inSocket.color
        inSocket.conn = this
        outSocket.conn = this
    }

    var color: Int

    private val f = Vector2()

    fun calcAttraction() {
        f.set(outSocket.absCoord()).sub(inSocket.absCoord()).setLength(attraction)
        inSocket.addForce(f)
        outSocket.addForce(f.scl(-1f))
    }

    fun clear() {
        inSocket.conn = null
        outSocket.conn = null
    }

    fun serialize(sb: StringBuilder) {
        sb.append(color).append(inSocket.ball.number, 3).append(inSocket.number)
            .append(outSocket.ball.number, 3).append(outSocket.number)
    }

}