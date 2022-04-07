package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.math.Vector2
import kotlin.random.Random

class Socket(val ball: Ball) {
    val coord = Vector2()
    val tri = Array(3) { Vector2() }
    var color: Int = Random.nextInt(7) + 1
    var conn: Connector? = null

    fun addForce(f: Vector2) {
        ball.torque += coord.crs(f)
        ball.addForce(f)
    }
}