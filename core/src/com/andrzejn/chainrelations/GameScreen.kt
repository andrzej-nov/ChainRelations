package com.andrzejn.chainrelations

import com.andrzejn.chainrelations.logic.Ball
import com.andrzejn.chainrelations.logic.World
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.math.plus
import kotlin.math.*

class GameScreen(val ctx: Context) : KtxScreen {
    val ballsCount = 60
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
        if (pointedBall != null) {
            val b = pointedBall ?: return
            ctx.sd.filledCircle(b.coord.x, b.coord.y, min(world.width, world.height) / 3, Color.DARK_GRAY)
        }
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
            ball.draw(ctx.batch)
            it.draw(ctx.sd, 1f, ctx.light)
        }
        ball.color = Color.GRAY
        if (pointedBall != null) {
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
        if (pointedBall != null && dragTo.x > 0) {
            ctx.sd.setColor(Color.WHITE)
            ctx.sd.line(pointedBall!!.outCom[0].coord.plus(pointedBall!!.coord), dragTo, world.bb.lineWidth * 2)
        }
        ctx.batch.end()
    }

    var pointedBall: Ball? = null
    var dragTo = Vector2(-1f, -1f)

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
                pointedBall = world.hitTest(v)
            }
            //println("${v.x} ${v.y} ${world.balls[0].coord.x} ${world.balls[0].coord.y}")
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * Called when screen is untouched (mouse button released). That's either a drag end or tile drop.
         */
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val v = ctx.pointerPosition(input.x, input.y)
            if (pointedBall != null && dragTo.x > 0) {
                setDragTo(v)
                val otherBall = world.hitTest(dragTo)
                if (otherBall != null && otherBall != pointedBall)
                    world.addConnector(pointedBall!!, otherBall)
            }
            pointedBall = null
            dragTo.set(-1f, -1f)
            return super.touchUp(screenX, screenY, pointer, button)
        }

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            setDragTo(ctx.pointerPosition(input.x, input.y))
            return super.touchDragged(screenX, screenY, pointer)
        }

        private fun setDragTo(v: Vector2) {
            if (pointedBall == null) return
            dragTo =
                v.sub(pointedBall!!.coord).clamp(0f, min(world.width, world.height) / 3f).add(pointedBall!!.coord)
        }
    }

}
