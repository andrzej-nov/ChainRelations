package com.andrzejn.chainrelations

import com.andrzejn.chainrelations.logic.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.math.minus

class GameScreen(val ctx: Context) : KtxScreen {
    val maxConnLen: Float = 4.5f // Maximum connector length, in ball radiuses

    init { // ballsCount n range 20..50
        ctx.wc = WorldConstants(20).also { it.setValues(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()) }
    }

    val world = World(ctx) // Create World after WorldConstants
    val ball = Sprite(ctx.ball)

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
        ctx.setCamera(width, height)
        world.resize(width.toFloat(), height.toFloat())
    }

    override fun hide() {
        super.hide()
        input.inputProcessor = null
    }

    fun ballBlinked(b: Ball) {
        calcSuitableTargets(pointedBall, dragFrom)
        if (suitableTargets?.isEmpty() == true)
            cleanDragState(false)
    }

    override fun render(delta: Float) {
        super.render(delta)
        world.moveBalls(delta)
        world.blinkRandomBall { b -> ballBlinked(b) }
        ctx.tweenManager.update(delta)
        ctx.batch.begin()
        val dF = dragFrom
        val pB = pointedBall
        if (dF != null) // Drag from connector in progress. Draw background drag limit circle.
            ctx.sd.filledCircle(dF.absDrawCoord(), ctx.wc.radius * maxConnLen, Color.DARK_GRAY)
        world.drawConnectors()
        world.balls.filter { it.inBlink }.forEach { it.setEyeCoords() }
        world.balls.filter { dF != null || it != pB }.forEach {
            setBallSpriteBounds(it.drawCoord, 1f)
            ball.color =
                if (dF != null && suitableTargets?.contains(it) == true) ctx.dark[dF.color] else Color.GRAY
            ball.draw(ctx.batch)
            it.drawElements()
        }
        ball.color = Color.GRAY
        if (pB != null && dF == null) { // Draw large pointed ball to pick a connector and start drag
            setBallSpriteBounds(pointedBallCenter, 2f)
            ball.draw(ctx.batch)
            pB.drawElements(2f, pointedBallCenter)
        }
        if (dF != null && dragTo.x > 0)
            ctx.sd.line(dF.absDrawCoord(), dragTo, ctx.light[dF.color], ctx.wc.lineWidth * 2)
        ctx.batch.end()
    }

    private fun setBallSpriteBounds(v: Vector2, k: Float) {
        val r = ctx.wc.radius
        ball.setBounds(v.x - r * k, v.y - r * k, r * 2 * k, r * 2 * k)
    }

    var pointedBallCenter = Vector2(-1f, -1f)
    var pointedBall: Ball? = null
    var dragTo = Vector2(-1f, -1f)
    var dragFrom: BaseSocket? = null
    var suitableTargets: Set<Ball>? = null

    fun pointTheBall(b: Ball?) {
        pointedBall = b
        if (b == null)
            return
        pointedBallCenter.set(b.drawCoord)
        world.clampCoord(pointedBallCenter, ctx.wc.radius * 2)
    }

    /**
     * Our input adapter
     */
    inner class IAdapter : InputAdapter() {

        /**
         * Handle presses/clicks
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (button == Input.Buttons.RIGHT) world.randomHit()
            else pointTheBall(world.ballPointedBy(ctx.pointerPosition(input.x, input.y)))
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val v = ctx.pointerPosition(input.x, input.y)
            if (pointedBall == null)
                pointTheBall(world.ballPointedBy(v))
            if (pointedBall != null && dragFrom == null) {
                setDragFrom(v)
                if (dragFrom == null && pointedBallCenter.dst(v) > ctx.wc.radius * 2)
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
            val dF = dragFrom
            var connectorAdded = false
            if (dF != null) {
                setDragTo(v)
                val otherBall = world.ballPointedBy(dragTo)
                if (otherBall != null && suitableTargets?.contains(otherBall) == true)
                    connectorAdded = world.addConnector(dF, otherBall)
            }
            val st = suitableTargets
            if (connectorAdded || st == null || st.isEmpty())
                cleanDragState(true)
            return super.touchUp(screenX, screenY, pointer, button)
        }

        private fun setDragFrom(v: Vector2) {
            val pB = pointedBall ?: return
            val v1 = v.minus(pointedBallCenter).scl(0.5f)
            val dF = pB.sockets.firstOrNull { it.conn == null && it.coord.epsilonEquals(v1, ctx.wc.radius * 0.3f) }
            dragFrom = dF
            if (dF == null)
                return
            calcSuitableTargets(pB, dF)
            if (suitableTargets?.isEmpty() == true) // No suitable targets, do not start drag from this socket
                dragFrom = null
        }

        private fun setDragTo(v: Vector2) {
            val dF = dragFrom ?: return
            val dragFromCoord = dF.absDrawCoord()
            dragTo = v.sub(dragFromCoord).clamp(0f, maxConnLen * ctx.wc.radius).add(dragFromCoord)
        }
    }

    fun cleanDragState(cleanPointedBall: Boolean) {
        if (cleanPointedBall)
            pointedBall = null
        dragFrom = null
        suitableTargets = null
        dragTo.set(-1f, -1f)
    }

    fun calcSuitableTargets(
        pB: Ball?,
        dF: BaseSocket?
    ) {
        if (pB == null || dF == null)
            return
        val dragFromCoord = dF.absDrawCoord()
        suitableTargets =
            world.balls.filter { b ->
                b != pB && b.coord.dst(dragFromCoord) < (maxConnLen + 1) * ctx.wc.radius // other balls in range
                        && b.sockets.mapNotNull { s -> s.conn }
                    .none { c -> c.inSocket.ball == pB || c.outSocket.ball == pB }
                        // not connected to the pointed ball yet
                        && (if (dF is InSocket) b.outSock else b.inSock)
                    .any { s ->
                        s.conn == null && s.color == dF.color && s.absDrawCoord()
                            .dst(dragFromCoord) < maxConnLen * ctx.wc.radius
                    } // and matching free connector in range
            }
                .toSet()
    }

}
