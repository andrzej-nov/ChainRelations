package com.andrzejn.chainrelations.logic

import com.badlogic.gdx.math.Vector2
import kotlin.random.Random

class World(val ballsCount: Int, radius: Float, var width: Float, var height: Float) {
    val wc = WorldConstants().also { it.setValues(radius) }
    val balls = List(ballsCount) { Ball(ballsCount, wc).also { it.setSocketCoords() } }

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
                clampCoord(it.coord, wc.radius)
            }
        }
    }

    fun clampCoord(crd: Vector2, rad: Float) {
        val br = rad * 1.1f
        crd.x = crd.x.coerceIn(br, width - br)
        crd.y = crd.y.coerceIn(br, height - br)
    }

    fun pointedBall(v: Vector2): Ball? {
        return balls.firstOrNull { it.coord.dst(v) < wc.radius }
    }

    fun addConnector(from: Socket, otherBall: Ball) {
        val fromBall = from.ball
        val con = Connector(
            from,
            (if (fromBall.inCom.contains(from)) otherBall.outCom else otherBall.inCom)
                .first { it.conn == null && it.color == from.color },
            wc.attraction
        )
        val ballsToClear = mutableListOf<Ball>()
        if (fromBall.inCom.plus(fromBall.outCom).none { it.conn == null })
            ballsToClear.add(fromBall)
        if (otherBall.inCom.plus(otherBall.outCom).none { it.conn == null })
            ballsToClear.add(otherBall)
        if (ballsToClear.size == 0) {
            connectors.add(con)
            return
        }
        ballsToClear.flatMap { it.inCom.asList().plus(it.outCom) }.mapNotNull { it.conn }.toMutableSet()
            .also { it.add(con) }.forEach {
                it.clear()
                connectors.remove(it)
            }
        ballsToClear.forEach { it.reset() }
    }

    fun randomHit() {
        balls.forEach {
            it.coord.add(Random.nextFloat() * 2 * wc.radius - wc.radius, Random.nextFloat() * 2 * wc.radius - wc.radius)
            clampCoord(it.coord, wc.radius)
        }
    }

    fun resize(bRadius: Float, width: Int, height: Int) {
        wc.setValues(bRadius)
        val kx = width.toFloat() / this.width
        val ky = height.toFloat() / this.height
        balls.forEach {
            it.coord.scl(kx, ky)
            it.setSocketCoords()
        }
        connectors.forEach {
            it.attraction = wc.attraction
        }
        this.width = width.toFloat()
        this.height = height.toFloat()
    }
}