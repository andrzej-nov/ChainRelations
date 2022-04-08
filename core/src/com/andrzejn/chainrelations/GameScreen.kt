package com.andrzejn.chainrelations

import com.andrzejn.chainrelations.logic.Ball
import com.andrzejn.chainrelations.logic.Socket
import com.andrzejn.chainrelations.logic.World
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.math.minus
import ktx.math.plus
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class GameScreen(val ctx: Context) : KtxScreen {
    val ballsCount = 30
    var bRadius = ballRadius(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    val world = World(ballsCount, bRadius, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    val ball = Sprite(ctx.ball)

    fun ballRadius(width: Float, height: Float): Float {
        val minSide = min(width, height)
        val maxSide = max(width, height)
        var count: Int = 1
        while (true) {
            val r = minSide / count
            val count2: Int = floor(maxSide / r).toInt()
            if (count * count2 >= ballsCount)
                return r / 2.6f // Empirical coefficient for optimal ball size
            count++
        }
    }

    /**
     * The input adapter instance for this screen
     */
    private val ia = IAdapter()

    override fun show() {
        super.show()
        input.inputProcessor = ia
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        bRadius = ballRadius(width.toFloat(), height.toFloat())
        world.resize(bRadius, width, height)
        ctx.setCamera(width, height)
    }

    override fun hide() {
        super.hide()
        input.inputProcessor = null
    }

    override fun render(delta: Float) {
        super.render(delta)
        world.moveBalls(delta, 20)
        ctx.batch.begin()
        if (dragFrom != null)
            ctx.sd.filledCircle(pointedBall!!.coord.plus(dragFrom!!.coord), world.wc.radius * 4.5f, Color.DARK_GRAY)
        world.connectors.forEach {
            ctx.sd.setColor(ctx.dark[it.color])
            ctx.sd.line(
                it.inSocket.coord.plus(it.inSocket.ball.coord),
                it.outSocket.coord.plus(it.outSocket.ball.coord),
                world.wc.lineWidth * 2
            )
        }
        world.balls.filter { dragFrom != null || it != pointedBall }.forEach {
            ball.setBounds(
                it.coord.x - bRadius,
                it.coord.y - bRadius,
                bRadius * 2,
                bRadius * 2
            )
            ball.color =
                if (dragFrom != null && suitableTargets?.contains(it) == true) ctx.dark[dragFrom!!.color] else Color.GRAY
            ball.draw(ctx.batch)
            it.draw(ctx.sd, 1f, ctx.light)
        }
        ball.color = Color.GRAY
        if (pointedBall != null && dragFrom == null) {
            val pb = pointedBall!!
            ball.setBounds(
                pointedBallCenter.x - bRadius * 2,
                pointedBallCenter.y - bRadius * 2,
                bRadius * 4,
                bRadius * 4
            )
            ball.draw(ctx.batch)
            pb.draw(ctx.sd, 2f, ctx.light, pointedBallCenter)
        }
        if (dragFrom != null && dragTo.x > 0)
            ctx.sd.line(
                dragFrom!!.coord.plus(pointedBall!!.coord),
                dragTo,
                ctx.light[dragFrom!!.color],
                world.wc.lineWidth * 2
            )
        ctx.batch.end()
    }

    var pointedBallCenter = Vector2(-1f, -1f)
    var pointedBall: Ball? = null
    var dragTo = Vector2(-1f, -1f)
    var dragFrom: Socket? = null
    var suitableTargets: Set<Ball>? = null

    /**
     * Our input adapter
     */
    inner class IAdapter : InputAdapter() {

        fun setPointedBall(b: Ball?) {
            pointedBall = b
            if (b == null)
                return
            pointedBallCenter.set(b.coord)
            world.clampCoord(pointedBallCenter, world.wc.radius * 2)
        }

        /**
         * Handle presses/clicks
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (button == Input.Buttons.RIGHT)
                world.randomHit()
            else {
                val v = ctx.pointerPosition(input.x, input.y)
                setPointedBall(world.pointedBall(v))
            }
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val v = ctx.pointerPosition(input.x, input.y)
            if (pointedBall == null)
                setPointedBall(world.pointedBall(v))
            if (pointedBall != null && dragFrom == null) {
                setDragFrom(v)
                if (dragFrom == null && pointedBallCenter.dst(v) > world.wc.radius * 2)
                    pointedBall = null
            }
            if (dragFrom != null)
                setDragTo(v)
            return super.touchDragged(screenX, screenY, pointer)
        }

        /**
         * Called when screen is untouched (mouse button released). That's either a drag end or tile drop.
         */
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val v = ctx.pointerPosition(input.x, input.y)
            if (dragFrom != null) {
                setDragTo(v)
                val otherBall = world.pointedBall(dragTo)
                if (otherBall != null && suitableTargets?.contains(otherBall) == true)
                    world.addConnector(dragFrom!!, otherBall)
            }
            pointedBall = null
            dragTo.set(-1f, -1f)
            dragFrom = null
            suitableTargets = null
            return super.touchUp(screenX, screenY, pointer, button)
        }

        private fun setDragFrom(v: Vector2) {
            val pb = pointedBall!!
            val v1 = v.minus(pointedBallCenter).scl(0.5f)
            dragFrom = pb.inCom.plus(pb.outCom)
                .firstOrNull { it.conn == null && it.coord.epsilonEquals(v1, pb.wc.radius * 0.15f) }
            if (dragFrom == null)
                return
            val isInCom = pb.inCom.contains(dragFrom)
            val dragFromCoord = dragFrom!!.coord.plus(pointedBall!!.coord)
            suitableTargets =
                world.balls.filter { b ->
                    b != pb && b.coord.dst(dragFromCoord) < 5.5f * pb.wc.radius
                            && b.inCom.plus(b.outCom).mapNotNull { s -> s.conn }
                        .none { c -> c.inSocket.ball == pb || c.outSocket.ball == pb }
                            && (if (isInCom) b.outCom else b.inCom)
                        .any { s ->
                            s.conn == null && s.color == dragFrom!!.color && s.coord.plus(b.coord)
                                .dst(dragFromCoord) < 4.5f * pb.wc.radius
                        }
                }
                    .toSet()
            if (suitableTargets?.size == 0)
                dragFrom = null
        }

        private fun setDragTo(v: Vector2) {
            if (dragFrom == null) return
            val dragFromCoord = dragFrom!!.coord.plus(pointedBall!!.coord)
            dragTo =
                v.sub(dragFromCoord).clamp(0f, 4.5f * world.wc.radius).add(dragFromCoord)
        }
    }

}
