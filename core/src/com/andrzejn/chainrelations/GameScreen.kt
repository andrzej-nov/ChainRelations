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
                return r / 2.6f
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
            ctx.sd.filledCircle(pointedBall!!.coord.plus(dragFrom!!.coord), world.bb.radius * 4.5f, Color.DARK_GRAY)
        world.connectors.forEach {
            ctx.sd.setColor(ctx.dark[it.color])
            ctx.sd.line(
                it.inSocket.coord.plus(it.inSocket.ball.coord),
                it.outSocket.coord.plus(it.outSocket.ball.coord),
                world.bb.lineWidth * 2
            )
        }
        world.balls.forEach {
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
                pb.coord.x - bRadius * 2,
                pb.coord.y - bRadius * 2,
                bRadius * 4,
                bRadius * 4
            )
            ball.draw(ctx.batch)
            pb.draw(ctx.sd, 2f, ctx.light)
        }
        if (dragFrom != null && dragTo.x > 0)
            ctx.sd.line(
                dragFrom!!.coord.plus(pointedBall!!.coord),
                dragTo,
                ctx.light[dragFrom!!.color],
                world.bb.lineWidth * 2
            )
        ctx.batch.end()
    }

    var pointedBall: Ball? = null
    var dragTo = Vector2(-1f, -1f)
    var dragFrom: Socket? = null
    var suitableTargets: Set<Ball>? = null

    /**
     * Our input adapter
     */
    inner class IAdapter : InputAdapter() {
        /**
         * Handle presses/clicks
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (button == Input.Buttons.RIGHT)
                world.randomHit()
            else {
                val v = ctx.pointerPosition(input.x, input.y)
                pointedBall = world.pointedBall(v)
            }
            return super.touchDown(screenX, screenY, pointer, button)
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

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val v = ctx.pointerPosition(input.x, input.y)
            if (pointedBall == null)
                pointedBall = world.pointedBall(v)
            if (pointedBall != null && dragFrom == null) {
                setDragFrom(v)
                if (dragFrom == null && pointedBall!!.coord.dst(v) > world.bb.radius * 2)
                    pointedBall = null
            }
            if (dragFrom != null)
                setDragTo(v)
            return super.touchDragged(screenX, screenY, pointer)
        }

        private fun setDragFrom(v: Vector2) {
            val pb = pointedBall!!
            val v1 = v.minus(pb.coord).scl(0.5f)
            dragFrom = pb.inCom.plus(pb.outCom)
                .firstOrNull { it.conn == null && it.coord.epsilonEquals(v1, pb.bb.radius * 0.15f) }
            if (dragFrom == null)
                return
            val isInCom = pb.inCom.contains(dragFrom)
            suitableTargets =
                world.balls.filter {
                    it != pb && it.coord.dst(dragFrom!!.coord.plus(pointedBall!!.coord)) < 4.5f * pb.bb.radius
                            && it.inCom.plus(it.outCom).mapNotNull { s -> s.conn }
                        .none { c -> c.inSocket.ball == pb || c.outSocket.ball == pb }
                            && (if (isInCom) it.outCom else it.inCom)
                        .any { s -> s.conn == null && s.color == dragFrom!!.color }
                }
                    .toSet()
        }

        private fun setDragTo(v: Vector2) {
            if (pointedBall == null) return
            dragTo =
                v.sub(pointedBall!!.coord).clamp(0f, 4.5f * world.bb.radius).add(pointedBall!!.coord)
        }
    }

}
