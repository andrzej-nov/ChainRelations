package com.andrzejn.chainrelations.logic

import com.andrzejn.chainrelations.Context
import com.badlogic.gdx.math.Vector2
import ktx.math.plus
import kotlin.random.Random

class World(val ctx: Context) {
    val balls = List(ctx.wc.ballsCount) { Ball(ctx).also { it.setSocketCoords() } }
    val connectors = mutableListOf<Connector>()

    init {
        balls.forEach {
            it.coord.set(
                Random.nextFloat() * (ctx.wc.width - 2 * ctx.wc.radius) + ctx.wc.radius,
                Random.nextFloat() * (ctx.wc.height - 2 * ctx.wc.radius) + ctx.wc.radius
            )
        }
    }

    fun resize(width: Float, height: Float) {
        val kx = width / ctx.wc.width
        val ky = height / ctx.wc.height
        ctx.wc.setValues(width, height)
        balls.forEach {
            it.coord.scl(kx, ky)
            it.setSocketCoords()
        }
        connectors.forEach {
            it.attraction = ctx.wc.attraction
        }
    }

    tailrec fun calcRepulsions(ball: List<Ball>) {
        if (ball.size <= 1)
            return
        val b = ball.drop(1)
        ball[0].calcRepulsions(b)
        this.calcRepulsions(b)
    }

    fun moveBalls(delta: Float) {
        val calcSteps = 20 // Enough for smooth calcuations on fps 60
        repeat(calcSteps) {
            balls.forEach { it.clearForces() }
            connectors.forEach { it.calcAttraction() }
            calcRepulsions(balls)
            balls.forEach {
                ctx.wc.applyBorderForce(it)
                it.moveBy(delta, calcSteps)
                clampCoord(it.coord, ctx.wc.radius)
            }
        }
    }

    fun clampCoord(crd: Vector2, rad: Float) {
        val br = rad * 1.1f // don't let the balls touch walls
        crd.set(
            crd.x.coerceIn(br, ctx.wc.width - br),
            crd.y.coerceIn(br, ctx.wc.height - br)
        )
    }

    fun ballPointedBy(v: Vector2): Ball? {
        return balls.firstOrNull { it.coord.dst(v) < ctx.wc.radius }
    }

    fun addConnector(from: BaseSocket, otherBall: Ball) {
        val fromBall = from.ball
        val con = if (from is InSocket)
            Connector(
                from,
                otherBall.outSock.first { it.conn == null && it.color == from.color },
                ctx.wc.attraction
            )
        else
            Connector(
                otherBall.inSock.first { it.conn == null && it.color == from.color },
                from as OutSocket,
                ctx.wc.attraction
            )
        val ballsToClear = mutableListOf<Ball>()
        if (fromBall.sockets.none { it.conn == null })
            ballsToClear.add(fromBall)
        if (otherBall.sockets.none { it.conn == null })
            ballsToClear.add(otherBall)
        if (ballsToClear.size == 0) {
            connectors.add(con)
            return
        }
        ballsToClear.flatMap { it.sockets }.mapNotNull { it.conn }.toMutableSet().also { it.add(con) }.forEach {
            it.clear()
            connectors.remove(it)
        }
        ballsToClear.forEach { it.reset() }
    }

    fun drawConnectors() = connectors.forEach {
        ctx.sd.setColor(ctx.dark[it.color])
        ctx.sd.line(
            it.inSocket.coord.plus(it.inSocket.ball.coord),
            it.outSocket.coord.plus(it.outSocket.ball.coord),
            ctx.wc.lineWidth * 2
        )
    }

    fun randomHit() {
        balls.forEach {
            it.coord.add(ctx.wc.randomCoordHit, ctx.wc.randomCoordHit)
            clampCoord(it.coord, ctx.wc.radius)
        }
    }
}