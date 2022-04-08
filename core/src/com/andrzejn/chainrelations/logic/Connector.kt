package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.math.Vector2
import ktx.math.minus
import ktx.math.plus
import kotlin.math.sqrt

class Connector {
    val inSocket: Socket
    val outSocket: Socket
    var attraction: Float

    constructor(inSocket: Socket, outSocket: Socket, attraction: Float) {
        this.inSocket = inSocket
        this.outSocket = outSocket
        this.attraction = attraction
        this.color = inSocket.color
        inSocket.conn = this
        outSocket.conn = this
    }

    val color: Int

    fun calcAttraction() {
        val f = Vector2(outSocket.coord).add(outSocket.ball.coord).sub(inSocket.coord).sub(inSocket.ball.coord).setLength(attraction)
        inSocket.addForce(f)
        outSocket.addForce(f.scl(-1f))
    }

    fun clear() {
        inSocket.conn = null
        outSocket.conn = null
    }

}