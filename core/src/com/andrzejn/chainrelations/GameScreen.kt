package com.andrzejn.chainrelations

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import com.andrzejn.chainrelations.helper.TW_POS_XY
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
import java.util.*

class GameScreen(val ctx: Context) : KtxScreen {
    var maxConnLen: Float = ctx.gs.maxRadius // Maximum connector length, in ball radiuses

    val ball = Sprite(ctx.ball)
    val play = Sprite(ctx.play).also { it.setAlpha(0.8f) }
    val home = Sprite(ctx.home).also { it.setAlpha(0.8f) }
    val help = Sprite(ctx.help).also { it.setAlpha(0.8f) }
    val exit = Sprite(ctx.exit).also { it.setAlpha(0.8f) }
    val hit = Sprite(ctx.hit).also { it.setAlpha(0.8f) }
    val hand = Sprite(ctx.hand).also { it.setAlpha(0.6f) }

    /**
     * The input adapter instance for this screen
     */
    private val ia = IAdapter()

    private var timeStart: Long = 0

    init {
        ctx.setTheme()
        ctx.wc = WorldConstants(ctx.gs.ballsCount).also {
            it.setValues(
                Gdx.graphics.width.toFloat(),
                Gdx.graphics.height.toFloat()
            )
        }
    }

    var world = World(ctx) // Create World only after WorldConstants

    fun newGame(loadSavedGame: Boolean) {
        ctx.score.reset()
        updateInGameDuration()
        timeStart = Calendar.getInstance().timeInMillis
        world = World(ctx)
        maxConnLen = ctx.gs.maxRadius
        if (loadSavedGame)
            try {
                val s = ctx.sav.savedGame()
                ctx.sav.loadSettingsAndScore(s)
                world.deserialize(s)
            } catch (ex: Exception) {
                // Something wrong. Just recreate new World and start new game
                world = World(ctx)
            }
        resize(ctx.wc.width.toInt(), ctx.wc.height.toInt())
    }

    override fun show() {
        super.show()
        input.inputProcessor = ia
        timeStart = Calendar.getInstance().timeInMillis
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        ctx.setCamera(width, height)
        world.resize(width.toFloat(), height.toFloat())
        val buttonSize = ctx.wc.buttonSize
        val fontHeight = ctx.wc.fontHeight
        ctx.score.setCoords(ctx.wc.fontHeight)
        play.setBounds(5f, fontHeight + 5 * buttonSize, buttonSize, buttonSize)
        help.setBounds(5f, fontHeight + 3 * buttonSize, buttonSize, buttonSize)
        hit.setBounds(5f, fontHeight + buttonSize, buttonSize, buttonSize)
        exit.setBounds(width - 5f - buttonSize, fontHeight + 3 * buttonSize, buttonSize, buttonSize)
        home.setBounds(width - 5f - buttonSize, fontHeight + buttonSize, buttonSize, buttonSize)
    }

    /**
     * Invoked when the screen is about to switch away, for any reason.
     * Update the in-game time and save records.
     */
    override fun hide() {
        super.hide()
        input.inputProcessor = null
        updateInGameDuration()
        ctx.score.saveRecords()
    }

    /**
     * Invoked when the screen is about to close, for any reason.
     * Update the in-game time and save records.
     */
    override fun pause() {
        updateInGameDuration()
        ctx.score.saveRecords()
        super.pause()
    }

    private fun updateInGameDuration() {
        ctx.gs.inGameDuration += Calendar.getInstance().timeInMillis - timeStart
        timeStart = Calendar.getInstance().timeInMillis
    }

    var inShowAMove = false

    private var lastGameSave: Long = 0

    fun autoSaveGame() {
        val t = Calendar.getInstance().timeInMillis
        if (t - lastGameSave < 5000)
            return
        ctx.sav.saveGame(world)
        lastGameSave = t
    }

    fun ballBlinked() {
        suitableTargets = calcSuitableTargets(pointedBall, dragFrom)
        if (suitableTargets?.isEmpty() == true)
            cleanDragState(false)
    }

    override fun render(delta: Float) {
        super.render(delta)
        try {
            world.moveBalls(delta)
            autoSaveGame()
            if (!inShowAMove)
                world.blinkRandomBall { ballBlinked() }
            ctx.tweenManager.update(delta)
            world.balls.filter { it.inBlink || it.inDeath }.forEach { it.updateEyeCoords() }
        } catch (ex: Exception) {
            // There should be no exceptions here. But if they are, simply restart the game.
            newGame(false)
        }
        if (!ctx.batch.isDrawing) ctx.batch.begin()
        ctx.sd.setColor(Color(Color.DARK_GRAY).also { it.a = 0.8f })
        ctx.sd.filledRectangle(0f, 0f, ctx.wc.buttonSize + 5f, ctx.wc.height)
        ctx.sd.filledRectangle(ctx.wc.width - ctx.wc.buttonSize - 5f, 0f, ctx.wc.buttonSize + 5f, ctx.wc.height)
        ctx.sd.filledRectangle(
            ctx.wc.buttonSize + 5f,
            0f,
            ctx.wc.width - 2 * (ctx.wc.buttonSize + 5f),
            ctx.wc.fontHeight.toFloat() + 5f
        )
        val dF = dragFrom
        val pB = pointedBall
        if (dF != null) // Drag from connector in progress. Draw background drag limits circle.
            ctx.sd.filledCircle(dF.absDrawCoord(), ctx.wc.radius * maxConnLen, Color.DARK_GRAY)
        world.drawConnectors()
        world.balls.filter { dF != null || it != pB }.forEach {
            setBallSpriteBounds(it.drawCoord, 1f)
            ball.color =
                if (dF != null && suitableTargets?.contains(it) == true) ctx.dark[dF.color] else Color.GRAY
            ball.draw(ctx.batch, it.alpha)
            it.drawDetails()
        }
        ball.color = Color.GRAY
        if (pB != null && dF == null) { // Draw pointed ball large to pick a connector and start drag
            setBallSpriteBounds(pointedBallCenter, 2f)
            ball.draw(ctx.batch, pB.alpha)
            pB.drawDetails(2f, pointedBallCenter)
        }
        if (dF != null && dragTo.x > 0) // Draw drag line to current pointed coords
            ctx.sd.line(dF.absDrawCoord(), dragTo, ctx.light[dF.color], ctx.wc.lineWidth * 2)
        else if (dF != null && inShowAMove) { // We are showing a move. Draw the line to the hand sprite
            val dFC = dF.absDrawCoord()
            ctx.sd.line(
                dFC.x,
                dFC.y,
                hand.x + hand.width / 2,
                hand.y + hand.height,
                ctx.light[dF.color],
                ctx.wc.lineWidth * 2
            )
        }
        play.draw(ctx.batch)
        help.draw(ctx.batch)
        hit.draw(ctx.batch)
        exit.draw(ctx.batch)
        home.draw(ctx.batch)
        ctx.score.draw(ctx.batch)
        if (inShowAMove)
            hand.draw(ctx.batch)
        if (ctx.batch.isDrawing) ctx.batch.end()
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

    private var dTforShow: Ball? = null

    fun showAMove() {
        cleanDragState(true)
        if (inShowAMove)
            return
        inShowAMove = true
        val dF = world.balls.filter { !it.inBlink && !it.inDeath }.flatMap { it.sockets }.filter { it.conn == null }
            .shuffled().firstOrNull { calcSuitableTargets(it.ball, it)?.isNotEmpty() == true }
        if (dF == null) {
            inShowAMove = false
            return
        }

        Timeline.createSequence()
            .push(Tween.call { _, _ ->
                hand.setPosition(
                    help.x + help.width / 2 - hand.width / 2,
                    help.y + help.height / 2 - hand.height
                )
            })
            .push(Tween.to(hand, TW_POS_XY, 1f).target(dF.ball.coord.x - hand.width / 2, dF.ball.coord.y - hand.height))
            .push(Tween.call { _, _ -> pointTheBall(dF.ball) })
            .setCallback { _, _ -> showAMoveMiddle(dF) }
            .start(ctx.tweenManager)
    }

    private fun showAMoveMiddle(dF: BaseSocket) {
        val dFCoord = Vector2(dF.coord).scl(2f).add(pointedBallCenter)
        Timeline.createSequence()
            .pushPause(0.2f)
            .push(Tween.to(hand, TW_POS_XY, 0.5f).target(dFCoord.x - hand.width / 2, dFCoord.y - hand.height))
            .pushPause(0.3f)
            .push(Tween.call { _, _ ->
                dragFrom = dF
                suitableTargets = calcSuitableTargets(dF.ball, dF)
                dTforShow = suitableTargets?.firstOrNull { !it.inDeath && !it.inBlink }
            })
            .setCallback { _, _ -> showAMoveFinalize() }
            .start(ctx.tweenManager)
    }

    fun showAMoveFinalize() {
        val dT = dTforShow
        if (dT == null) {
            cleanDragState(true)
            inShowAMove = false
            return
        }
        Timeline.createSequence()
            .push(
                Tween.to(hand, TW_POS_XY, 1f)
                    .target(dT.coord.x - hand.width / 2, dT.coord.y - hand.height)
            )
            .pushPause(0.2f)
            .push(Tween.call { _, _ ->
                val dF = dragFrom
                if (dF != null)
                    world.addConnector(dF, dT)
                cleanDragState(true)
                inShowAMove = false
            })
            .start(ctx.tweenManager)
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
    ): Set<Ball>? {
        if (pB == null || dF == null)
            return null
        val dragFromCoord = dF.absDrawCoord()
        return world.balls.filter { b ->
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

    /**
     * Our input adapter
     */
    inner class IAdapter : InputAdapter() {

        /**
         * Handle presses/clicks
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (inShowAMove) return super.touchDown(screenX, screenY, pointer, button)
            if (button == Input.Buttons.RIGHT) world.randomHit()
            else pointTheBall(world.ballPointedBy(ctx.pointerPosition(input.x, input.y)))
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (inShowAMove) return super.touchDragged(screenX, screenY, pointer)
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
         * Called when screen is untouched (mouse button released)
         */
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (inShowAMove) return super.touchUp(screenX, screenY, pointer, button)
            val v = ctx.pointerPosition(input.x, input.y)
            val dF = dragFrom
            if (dF != null) {
                setDragTo(v)
                val otherBall = world.ballPointedBy(dragTo)
                if (otherBall != null && suitableTargets?.contains(otherBall) == true)
                    world.addConnector(dF, otherBall)
            }
            cleanDragState(true)
            when {
                buttonTouched(v, play) -> newGame(false)
                buttonTouched(v, exit) -> Gdx.app.exit()
                buttonTouched(v, hit) -> world.randomHit()
                buttonTouched(v, help) -> showAMove()
            }
            return super.touchUp(screenX, screenY, pointer, button)
        }

        private fun buttonTouched(v: Vector2, s: Sprite) = v.x in s.x..s.x + s.width && v.y in s.y..s.y + s.height

        private fun setDragFrom(v: Vector2) {
            val pB = pointedBall ?: return
            val v1 = v.minus(pointedBallCenter).scl(0.5f)
            val dF = pB.sockets.firstOrNull { it.conn == null && it.coord.epsilonEquals(v1, ctx.wc.radius * 0.3f) }
            dragFrom = dF
            if (dF == null)
                return
            suitableTargets = calcSuitableTargets(pB, dF)
            if (suitableTargets?.isEmpty() == true) // No suitable targets, do not start drag from this socket
                dragFrom = null
        }

        private fun setDragTo(v: Vector2) {
            val dF = dragFrom ?: return
            val dragFromCoord = dF.absDrawCoord()
            dragTo = v.sub(dragFromCoord).clamp(0f, maxConnLen * ctx.wc.radius).add(dragFromCoord)
        }
    }
}
