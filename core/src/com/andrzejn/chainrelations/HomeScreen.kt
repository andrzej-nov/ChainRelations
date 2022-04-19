package com.andrzejn.chainrelations

import com.andrzejn.chainrelations.helper.Slider
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import ktx.app.KtxScreen
import kotlin.math.min

/**
 * The Home/Settings screen.
 * First screen of the application. Displayed by the Main class after the application is created.
 * (unless there is a saved game, then we go directly to the game screen with the resumed game).
 */
class HomeScreen(
    /**
     * Reference to the main scenn context
     */
    val ctx: Context
) : KtxScreen {
    private val ia = IAdapter()
    private var fontItems: BitmapFont = BitmapFont()
    private lateinit var fcItems: BitmapFontCache

    /**
     * Called by the GDX framework on screen change to this screen. When Home screen is shown,
     * we clear the saved game (going Home means the current game is abandoned)
     * and switch on the screen input processor.
     */
    override fun show() {
        super.show()
        ctx.sav.clearSavedGame()
        Gdx.input.inputProcessor = ia
        Gdx.graphics.isContinuousRendering = false
    }

    /**
     * Called by GDX runtime on screen hide.
     */
    override fun hide() {
        super.hide()
        Gdx.input.inputProcessor = null // Detach the input processor
    }

    private val logo = Sprite(ctx.logo)
    private val play = Sprite(ctx.play)
    private val exit = Sprite(ctx.exit)
    private val options = Sprite(ctx.info)
    private val gear = Sprite(ctx.gear)
    private val darktheme = Sprite(ctx.darktheme)
    private val lighttheme = Sprite(ctx.lighttheme)
    private val smallballs = Sprite(ctx.smallballs)
    private val largeballs = Sprite(ctx.largeballs)
    private val smallradius = Sprite(ctx.smallradius)
    private val largeradius = Sprite(ctx.largeradius)
    private val colors6 = Sprite(ctx.colors6)
    private val colors7 = Sprite(ctx.colors7)
    private val sBallsCount = Slider(ctx, 20f, 60f, ctx.gs.ballsCount.toFloat(), 1f)
    private val sMaxRadius = Slider(ctx, 3f, 6f, ctx.gs.maxRadius, 0.1f)
    private val recycle = Sprite(ctx.recycle)
    private val death = Sprite(ctx.death)

    private var gridX = 0f
    private var gridY = 0f
    private val lineWidth = 2f
    private var radius = 0f
    private var baseX = 0f
    private var baseWidth = 0f

    /**
     * Called by the GDX framework on screen resize (window resize, device rotation). Triggers all subsequent
     * coordinates recalculations and layout changes.
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        ctx.setCamera(width, height)
        val baseHeight = ctx.camera.viewportHeight
        baseWidth = min(ctx.camera.viewportWidth, baseHeight * 3 / 4)
        baseX = (ctx.camera.viewportWidth - baseWidth) / 2
        gridX = baseWidth / 12
        gridY = baseHeight / 9
        radius = min(2 * gridX, gridY) * 0.4f

        ctx.fitToRect(logo, baseWidth, 2 * gridY * 0.8f)
        logo.setPosition(
            (baseWidth - logo.width) / 2 + baseX,
            gridY * 8 - logo.height / 2
        )

        sBallsCount.setBounds(2 * gridX + baseX, gridY * 6, 8 * gridX, gridY)
        ctx.fitToRect(largeballs, gridX, gridY)
        largeballs.setPosition(
            gridX - largeballs.width / 2 + baseX,
            gridY * 6
        )
        ctx.fitToRect(smallballs, gridX, gridY)
        smallballs.setPosition(
            gridX * 11 - smallballs.width / 2 + baseX,
            gridY * 6
        )
        sMaxRadius.setBounds(2 * gridX + baseX, gridY * 5, 8 * gridX, gridY)
        ctx.fitToRect(smallradius, gridX, gridY)
        smallradius.setPosition(
            gridX - smallradius.width / 2 + baseX,
            gridY * 5
        )
        ctx.fitToRect(largeradius, gridX, gridY)
        largeradius.setPosition(
            gridX * 11 - largeradius.width / 2 + baseX,
            gridY * 5
        )
        ctx.fitToRect(colors6, 3 * gridX * 0.7f, gridY * 0.7f)
        colors6.setPosition(
            4 * gridX - colors6.width / 2 + baseX,
            gridY * 4 + (gridY - colors6.height) / 2
        )
        ctx.fitToRect(colors7, 3 * gridX * 0.7f, gridY * 0.7f)
        colors7.setPosition(
            8 * gridX - colors7.width / 2 + baseX,
            gridY * 4 + (gridY - colors7.height) / 2
        )
        ctx.fitToRect(recycle, 3 * gridX * 0.7f, gridY * 0.7f)
        recycle.setPosition(
            4 * gridX - recycle.width / 2 + baseX,
            gridY * 3 + (gridY - recycle.height) / 2
        )
        ctx.fitToRect(death, 3 * gridX * 0.7f, gridY * 0.7f)
        death.setPosition(
            8 * gridX - death.width / 2 + baseX,
            gridY * 3 + (gridY - death.height) / 2
        )
        ctx.fitToRect(darktheme, 3 * gridX * 0.7f, gridY * 0.7f)
        darktheme.setPosition(
            4 * gridX - darktheme.width / 2 + baseX,
            gridY * 2 + (gridY - darktheme.height) / 2
        )
        ctx.fitToRect(lighttheme, 3 * gridX * 0.7f, gridY * 0.7f)
        lighttheme.setPosition(
            8 * gridX - lighttheme.width / 2 + baseX,
            gridY * 2 + (gridY - lighttheme.height) / 2
        )

        fontItems.dispose()
        fontItems = ctx.createFont((gridY * 0.3f).toInt())
        fcItems = BitmapFontCache(fontItems)
        fcItems.addText("1.", baseX * 0.2f, gridY * 6 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("2.", baseX * 0.2f, gridY * 5 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("3.", baseX * 0.2f, gridY * 4 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("4.", baseX * 0.2f, gridY * 3 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("5.", baseX * 0.2f, gridY * 2 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.setColors(ctx.theme.settingItem)

        ctx.fitToRect(gear, 2 * gridX * 0.5f, gridY * 0.5f)
        ctx.fitToRect(play, 4 * gridX * 0.8f, 2 * gridY * 0.8f)
        play.setPosition(
            6 * gridX - play.width / 2 + baseX,
            (2 * gridY - play.height) / 2
        )
        ctx.fitToRect(exit, 2 * gridX * 0.8f, gridY * 0.8f)
        exit.setPosition(
            11 * gridX - exit.width / 2 + baseX,
            (gridY - exit.height) / 2
        )
        ctx.fitToRect(options, 2 * gridX * 0.8f, gridY * 0.8f)
        options.setPosition(
            gridX - options.width / 2 + baseX,
            (gridY - options.height) / 2
        )
    }

    /**
     * Called by the system each time when the screen needs to be redrawn. It is invoked very frequently,
     * especially when animations are running, so do not create any objects here and precalculate everything
     * as much as possible.
     */
    override fun render(delta: Float) {
        super.render(delta)
        ctx.batch.begin()
        ctx.sd.filledRectangle(0f, 0f, ctx.camera.viewportWidth, ctx.camera.viewportHeight, ctx.theme.screenBackground)
        ctx.sd.rectangle(baseX, 0f, baseWidth, ctx.camera.viewportHeight, ctx.theme.settingSeparator)
        logo.draw(ctx.batch)
        renderGameSettings()

        sBallsCount.render()
        sMaxRadius.render()
        smallballs.draw(ctx.batch)
        largeballs.draw(ctx.batch)
        smallradius.draw(ctx.batch)
        largeradius.draw(ctx.batch)
        colors6.draw(ctx.batch)
        colors7.draw(ctx.batch)
        darktheme.draw(ctx.batch)
        lighttheme.draw(ctx.batch)
        recycle.draw(ctx.batch)
        death.draw(ctx.batch)
        if (baseX / fontItems.lineHeight > 15f / 22f)
            fcItems.draw(ctx.batch)

        ctx.sd.line(
            baseX,
            7 * gridY,
            12 * gridX + baseX,
            7 * gridY,
            ctx.theme.settingSeparator,
            lineWidth
        )
        ctx.sd.line(
            baseX,
            1.9f * gridY,
            12 * gridX + baseX,
            1.9f * gridY,
            ctx.theme.settingSeparator,
            lineWidth
        )

        gear.setPosition(-gear.width / 2 + baseX, 7 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(12 * gridX - gear.width / 2 + baseX, 7 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(-gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(12 * gridX - gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        play.draw(ctx.batch)
        exit.draw(ctx.batch)
        options.draw(ctx.batch)
        ctx.batch.end()
    }

    /**
     * Render current game settings. When clicked/pressed, the settings changes are immediately saved and displayed.
     */
    private fun renderGameSettings() {
        sMaxRadius.normalizeValue(ctx.gs.maxRadius)
        sBallsCount.normalizeValue(ctx.gs.ballsCount.toFloat())

        var y = gridY * 4.05f
        var x = gridX * ((if (ctx.gs.colorsCount == 6) 3f else 7f) - 0.2f) + baseX
        ctx.sd.filledRectangle(
            x,
            y,
            2.4f * gridX,
            gridY * 0.9f,
            0f,
            ctx.theme.settingSelection, ctx.theme.settingSelection
        )

        y -= gridY
        x = gridX * ((if (ctx.gs.isRecycle) 3f else 7f) - 0.2f) + baseX
        ctx.sd.filledRectangle(
            x,
            y,
            2.4f * gridX,
            gridY * 0.9f,
            0f,
            ctx.theme.settingSelection, ctx.theme.settingSelection
        )

        y -= gridY
        x = gridX * ((if (ctx.gs.isDarkTheme) 3f else 7f) - 0.2f) + baseX
        ctx.sd.filledRectangle(
            x,
            y,
            2.4f * gridX,
            gridY * 0.9f,
            0f,
            ctx.theme.settingSelection, ctx.theme.settingSelection
        )
    }

    /**
     * Clean up
     */
    override fun dispose() {
        sBallsCount.dispose()
        sMaxRadius.dispose()
        super.dispose()
    }

    /**
     * The input adapter for this screen
     */
    inner class IAdapter : InputAdapter() {

        /**
         * Handle dragging
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            dragSliders()
            return super.touchDragged(screenX, screenY, pointer)
        }

        /**
         * Process clicks/presses. Change the settings as selected, or switch to another screen
         * (at the end of the method)
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val v = dragSliders()
            if (v.x < 0 || v.y < 0)
                return super.touchDown(screenX, screenY, pointer, button)
            v.x -= baseX

            if (v.y in 4 * gridY..5 * gridY) {
                if (v.x in 3 * gridX..5 * gridX)
                    ctx.gs.colorsCount = 6
                else if (v.x in 7 * gridX..9 * gridX)
                    ctx.gs.colorsCount = 7
            } else if (v.y in 3 * gridY..4 * gridY) {
                if (v.x in 3 * gridX..5 * gridX)
                    ctx.gs.isRecycle = true
                else if (v.x in 7 * gridX..9 * gridX)
                    ctx.gs.isRecycle = false
            } else if (v.y in 2 * gridY..3 * gridY) {
                if (v.x in 3 * gridX..5 * gridX) {
                    ctx.gs.isDarkTheme = true
                    ctx.setTheme()
                } else if (v.x in 7 * gridX..9 * gridX) {
                    ctx.gs.isDarkTheme = false
                    ctx.setTheme()
                }
            } else if (v.y < 2 * gridY && v.x in 5 * gridX..7 * gridX) {
                ctx.game.getScreen<GameScreen>().newGame(false)
                ctx.game.setScreen<GameScreen>()
            } else if (v.y < gridY && v.x > 10 * gridX)
                Gdx.app.exit()
            else if (v.y < gridY && v.x < 2 * gridX)
                ctx.game.setScreen<CreditsScreen>()
            return super.touchDown(screenX, screenY, pointer, button)
        }

        private fun dragSliders(): Vector2 {
            val v = ctx.pointerPosition(Gdx.input.x, Gdx.input.y)

            if (v.x in sBallsCount.x..sBallsCount.x + sBallsCount.width
                && v.y in sBallsCount.y..sBallsCount.y + sBallsCount.height
            ) {
                sBallsCount.touch(v.x)
                ctx.gs.ballsCount = sBallsCount.value.toInt()
                v.set(-1f, -1f)
            } else if (v.x in sMaxRadius.x..sMaxRadius.x + sMaxRadius.width
                && v.y in sMaxRadius.y..sMaxRadius.y + sMaxRadius.height
            ) {
                sMaxRadius.touch(v.x)
                ctx.gs.maxRadius = sMaxRadius.value
                v.set(-1f, -1f)
            }
            return v
        }
    }

}