package com.andrzejn.chainrelations.logic

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import com.andrzejn.chainrelations.Context
import com.andrzejn.chainrelations.helper.TW_ALPHA
import com.andrzejn.chainrelations.helper.TW_EYE_HK
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import java.util.*
import kotlin.random.Random

/**
 * The main game world and logic
 */
class World(
    /**
     * Reference to the main app context
     */
    val ctx: Context
) {
    private var cnt = 1

    /**
     * The list of the balls in game
     */
    var balls: MutableList<Ball> = MutableList(ctx.wc.ballsCount) { Ball(ctx, cnt++).also { it.setElementCoords() } }

    /**
     * The ball connectors
     */
    private val connectors = mutableListOf<Connector>()

    init {
        val width = ctx.wc.width - 2 * ctx.wc.radius - 2 * ctx.wc.buttonSize
        val offsetX = ctx.wc.radius + ctx.wc.buttonSize
        val height = ctx.wc.height - 2 * ctx.wc.radius
        val offsetY = ctx.wc.radius
        balls.forEach {
            it.coord.set(
                Random.nextFloat() * width + offsetX,
                Random.nextFloat() * height + offsetY
            )
        }
    }

    /**
     * Update layout for the current window size
     */
    fun resize(width: Float, height: Float) {
        val buttonSize = ctx.wc.buttonSize
        val kx = (width - 2 * buttonSize) / (ctx.wc.width - 2 * buttonSize)
        val ky = height / ctx.wc.height
        ctx.wc.setValues(width, height)
        balls.forEach {
            it.coord.sub(buttonSize, 0f).scl(kx, ky).add(buttonSize, 0f)
            it.setElementCoords(true)
        }
        connectors.forEach { it.attraction = ctx.wc.attraction }
    }

    /**
     * Calculate repulsion forces for all balls
     */
    private tailrec fun calcRepulsions(ball: List<Ball>) {
        if (ball.size <= 1)
            return
        val b = ball.drop(1)
        ball[0].calcRepulsions(b)
        this.calcRepulsions(b)
    }

    /**
     * Apply all ball forces and move them. Performs 20 steps of the numerical integration, it is just enough
     * for smooth ball moving.
     */
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

    /**
     * Ensure the ball remains on the screen.
     */
    fun clampCoord(crd: Vector2, rad: Float) {
        val br = rad * 1.1f // don't let the balls touch walls
        crd.set(
            crd.x.coerceIn(br + ctx.wc.buttonSize, ctx.wc.width - br - ctx.wc.buttonSize),
            crd.y.coerceIn(br, ctx.wc.height - br)
        )
    }

    /**
     * Returns the ball pointed by the coords, or null if there is no ball.
     */
    fun ballPointedBy(v: Vector2): Ball? = balls.firstOrNull { !it.inDeath && it.coord.dst(v) < ctx.wc.radius }

    /**
     * Add the connector from the socket to the ball. Updates scores, triggers death of fully connected balls if any
     */
    fun addConnector(from: BaseSocket, otherBall: Ball, deathCallback: () -> Unit): Boolean {
        val fromBall = from.ball
        val con = if (from is InSocket) {
            val outSocket = otherBall.outSock.firstOrNull { it.conn == null && it.color == from.color } ?: return false
            Connector(from, outSocket, ctx.wc.attraction)
        } else {
            val inSocket = otherBall.inSock.firstOrNull { it.conn == null && it.color == from.color } ?: return false
            Connector(inSocket, from as OutSocket, ctx.wc.attraction)
        }
        ctx.score.incrementMoves()
        val ballsToClear = mutableListOf<Ball>()
        if (fromBall.sockets.none { it.conn == null })
            ballsToClear.add(fromBall)
        if (otherBall.sockets.none { it.conn == null })
            ballsToClear.add(otherBall)
        if (ballsToClear.isEmpty()) {
            connectors.add(con)
            return true
        }
        ctx.score.addPoints(ballsToClear.size)
        ballsToClear.forEach { b ->
            val seq = Timeline.createSequence()
                .push(Tween.call { _, _ -> b.inDeath = true })
                .beginParallel()
                .push(Tween.to(b, TW_ALPHA, 1f).target(0f))
                .push(Tween.to(b, TW_EYE_HK, 0.5f).target(0f))
                .end()
            if (ctx.gs.isRecycle) {
                seq.push(Tween.call { _, _ -> b.reset() })
                    .beginParallel()
                    .push(Tween.to(b, TW_ALPHA, 1f).target(1f))
                    .push(Tween.to(b, TW_EYE_HK, 1f).target(1f))
                    .end()
                    .setCallback { _, _ -> b.inDeath = false }
            } else
                seq.setCallback { _, _ ->
                    balls.remove(b)
                    deathCallback()
                }
            seq.start(ctx.tweenManager)
        }
        ballsToClear.flatMap { it.sockets }.mapNotNull { it.conn }.toMutableSet().also { it.add(con) }.forEach {
            it.clear()
            connectors.remove(it)
        }
        return true
    }

    /**
     * Renders all ball connectors
     */
    fun drawConnectors(): Unit = connectors.forEach {
        ctx.sd.setColor(ctx.theme.dark[it.color])
        ctx.sd.line(
            it.inSocket.absDrawCoord(),
            it.outSocket.absDrawCoord(),
            ctx.wc.lineWidth * 2
        )
    }

    /**
     * Hit the field, shifting balls by random offsets (to give an inconveniently stuck ball a chance to reposition)
     */
    fun randomHit(): Unit = balls.forEach {
        it.coord.add(ctx.wc.randomCoordHit, ctx.wc.randomCoordHit)
        clampCoord(it.coord, ctx.wc.radius)
    }

    private var lastBlinkTime: Long = 0

    /**
     * Picks a random ball and initiates its blinking, with a random free socket color change
     */
    fun blinkRandomBall(ballBlinked: () -> Unit) {
        val t = Calendar.getInstance().timeInMillis
        if (t - lastBlinkTime < 3000)
            return
        lastBlinkTime = t
        ctx.wc.rotation = Gdx.input.rotation // Actually it is not related to blinking, but rotation check is costly
        // procedure, so it must be checked rarely
        val b = balls.filter { !it.inBlink && !it.inDeath }.random()
        Timeline.createSequence()
            .push(Tween.call { _, _ -> b.inBlink = true })
            .push(Tween.to(b, TW_EYE_HK, 0.3f).target(0f))
            .push(Tween.to(b, TW_EYE_HK, 0.2f).target(1f))
            .setCallback { _, _ ->
                b.inBlink = false
                b.recolorRandomSocket()
                ballBlinked()
            }
            .start(ctx.tweenManager)
    }

    /**
     * Serialize the whole world for game save
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(ctx.wc.width.toInt(), 4).append(ctx.wc.height.toInt(), 4)
        sb.append(balls.size, 2)
        balls.forEach { it.serialize(sb) }
        sb.append(connectors.size, 3)
        connectors.forEach { it.serialize(sb) }
    }

    /**
     * Deserialize the whole world for game load
     */
    fun deserialize(s: String) {
        val width = s.substring(17..20).toFloat()
        val height = s.substring(21..24).toFloat()
        ctx.wc.setValues(width, height)
        val ballsCount = s.substring(25..26).toInt()
        balls = MutableList(ballsCount) { Ball(ctx, 0) }
        var i = 27
        val bi = balls.iterator()
        repeat(ballsCount) { i = bi.next().deserialize(s, i) }
        connectors.clear()
        val connCount = s.substring(i..i + 2).toInt()
        i += 3
        repeat(connCount) {
            val c = s[i].digitToInt()
            val inBallNumber = s.substring(i + 1..i + 3).toInt()
            val inSockNumber = s[i + 4].digitToInt()
            val outBallNumber = s.substring(i + 5..i + 7).toInt()
            val outSockNumber = s[i + 8].digitToInt()
            val conn =
                Connector(balls.first { it.number == inBallNumber }
                    .sockets.first { it.number == inSockNumber } as InSocket,
                    balls.first { it.number == outBallNumber }
                        .sockets.first { it.number == outSockNumber } as OutSocket,
                    ctx.wc.attraction
                )
            conn.color = c
            connectors.add(conn)
            i += 9
        }
    }
}