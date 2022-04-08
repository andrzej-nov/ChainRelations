package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.math.Vector2
import kotlin.random.Random

class Socket(val ball: Ball, val isInCom: Boolean) {
    var color: Int = 0
    val coord = Vector2()
    val mark = Array(4) { Vector2() }
    var conn: Connector? = null

    fun addForce(f: Vector2) {
        ball.torque += coord.crs(f)
        ball.addForce(f)
    }
}