package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.math.Vector2
import kotlin.random.Random

class World(val ballsCount: Int, radius: Float, var width: Float, var height: Float) {
    val bb = BaseBall().also { it.setValues(radius) }
    val balls = List(ballsCount) { Ball(ballsCount, bb).also { it.setSocketCoords() } }

    val connectors = mutableListOf<Connector>()

    init {
        balls.forEach {
            it.coord.x = Random.nextFloat() * (width - 2 * radius) + radius
            it.coord.y = Random.nextFloat() * (height - 2 * radius) + radius
        }
    }

    tailrec fun calcRepulsions(ball: List<Ball>) {
        if (ball.size <= 1)
            return
        val b = ball.drop(1)
        ball[0].calcRepulsions(b)
        calcRepulsions(b)
    }

    fun moveBalls(delta: Float, steps: Int) {
        repeat(steps) {
            balls.forEach { it.clearForces() }
            connectors.forEach { it.calcAttraction() }
            calcRepulsions(balls)
            balls.forEach {
                it.applyBorderForce(width, height)
                it.moveBy(delta, 1f / steps)
                clampCoord(it)
            }
        }
    }

    fun clampCoord(b: Ball) {
        val br = bb.radius * 1.1f
        b.coord.x = b.coord.x.coerceIn(br, width - br)
        b.coord.y = b.coord.y.coerceIn(br, height - br)
    }

    fun hitTest(v: Vector2): Ball? = balls.firstOrNull { it.coord.dst(v) < bb.radius }

    fun addConnector(pointedBall: Ball, otherBall: Ball) =
        connectors.add(Connector(pointedBall.inCom[0], otherBall.outCom[0], bb.attraction))

    fun randomHit() {
        balls.forEach {
            it.coord.add(Random.nextFloat() * 100f - 50f, Random.nextFloat() * 2 * bb.radius - bb.radius)
            clampCoord(it)
        }
    }

    fun resize(bRadius: Float, width: Int, height: Int) {
        bb.setValues(bRadius)
        val kx = width.toFloat() / this.width
        val ky = height.toFloat() / this.height
        balls.forEach {
            it.coord.scl(kx, ky)
            it.setSocketCoords()
        }
        connectors.forEach {
            it.attraction = bb.attraction
        }
        this.width = width.toFloat()
        this.height = height.toFloat()
    }
}