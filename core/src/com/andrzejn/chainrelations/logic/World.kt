package com.andrzejn.chainrelations.logic

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import com.andrzejn.chainrelations.Context
import com.andrzejn.chainrelations.TW_ALPHA
import com.andrzejn.chainrelations.TW_EYE_HK
import com.badlogic.gdx.math.Vector2
import java.util.*
import kotlin.random.Random

class World(val ctx: Context) {
    val balls = List(ctx.wc.ballsCount) { Ball(ctx).also { it.setElementCoords() } }
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
            it.setElementCoords(true)
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
                it.applyAccelerometers()
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
        return balls.firstOrNull { !it.inDeath && it.coord.dst(v) < ctx.wc.radius }
    }

    fun addConnector(from: BaseSocket, otherBall: Ball): Boolean {
        val fromBall = from.ball
        val con = if (from is InSocket) {
            val outSocket = otherBall.outSock.firstOrNull { it.conn == null && it.color == from.color } ?: return false
            Connector(from, outSocket, ctx.wc.attraction)
        } else {
            val inSocket = otherBall.inSock.firstOrNull() { it.conn == null && it.color == from.color } ?: return false
            Connector(inSocket, from as OutSocket, ctx.wc.attraction)
        }
        val ballsToClear = mutableListOf<Ball>()
        if (fromBall.sockets.none { it.conn == null })
            ballsToClear.add(fromBall)
        if (otherBall.sockets.none { it.conn == null })
            ballsToClear.add(otherBall)
        if (ballsToClear.isEmpty()) {
            connectors.add(con)
            return true
        }
        ballsToClear.forEach { b ->
            Timeline.createSequence()
                .push(Tween.call { _, _ -> b.inDeath = true })
                .beginParallel()
                .push(Tween.to(b, TW_ALPHA, 1f).target(0f))
                .push(Tween.to(b, TW_EYE_HK, 0.5f).target(0f))
                .end()
                .push(Tween.call { _, _ -> b.reset() })
                .beginParallel()
                .push(Tween.to(b, TW_ALPHA, 1f).target(1f))
                .push(Tween.to(b, TW_EYE_HK, 1f).target(1f))
                .end()
                .setCallback { _, _ -> b.inDeath = false }
                .start(ctx.tweenManager)
        }
        ballsToClear.flatMap { it.sockets }.mapNotNull { it.conn }.toMutableSet().also { it.add(con) }.forEach {
            it.clear()
            connectors.remove(it)
        }
        return true
    }

    fun drawConnectors() = connectors.forEach {
        ctx.sd.setColor(ctx.dark[it.color])
        ctx.sd.line(
            it.inSocket.absDrawCoord(),
            it.outSocket.absDrawCoord(),
            ctx.wc.lineWidth * 2
        )
    }

    fun randomHit() {
        balls.forEach {
            it.coord.add(ctx.wc.randomCoordHit, ctx.wc.randomCoordHit)
            clampCoord(it.coord, ctx.wc.radius)
        }
    }

    private var lastBlinkTime: Long = 0

    fun blinkRandomBall(ballBlinked: () -> Unit) {
        val t = Calendar.getInstance().timeInMillis
        if (t - lastBlinkTime < 3000)
            return
        lastBlinkTime = t
        val b = balls.filter { !it.inBlink && !it.inDeath }.random()
        Timeline.createSequence()
            .push(Tween.call { _, _ -> b.inBlink = true })
            .push(Tween.to(b, TW_EYE_HK, 0.3f).target(0f))
            .push(Tween.to(b, TW_EYE_HK, 0.2f).target(1f))
            .push(Tween.call { _, _ ->
                b.inBlink = false
                b.recolorRandomSocket()
                ballBlinked()
            })
            .start(ctx.tweenManager)
    }
}