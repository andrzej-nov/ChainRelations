package com.andrzejn.chainrelations

import com.badlogic.gdx.Gdx.graphics
import ktx.app.KtxGame
import ktx.app.KtxScreen

/**
 * [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.
 * That is the application entry point.
 */
class Main : KtxGame<KtxScreen>() {
    private val ctx = Context(this) // Holds all application-wide objects.
    // Singleton objects cause a lot of issues on Android because of its memory allocation/release strategy,
    // so everything should be passed in the Context object on each app object creation or method call
    // where it is needed.

    /**
     * That is the first method called on application start.
     * On mobile devices, it is called each time when the application is restored/relaunched from background.
     */
    override fun create() {
        graphics.isContinuousRendering = true // This game does not require continuous screen rendering as it is
        // mostly static. So let's switch to rendering on demand, to save CPU resources.
        ctx.reloadAtlas()
        ctx.initBatch() // OpegGL batch objects are heavy. Usually you just need to create one or few of them
        // on the app start and retain them until the end
        addScreen(GameScreen(ctx))
        setScreen<GameScreen>()
        graphics.requestRendering() // Request first screen redraw.
    }

    override fun resume() {
        super.resume()
        ctx.reloadAtlas()
        ctx.initBatch()
    }

    /**
     * To avoid "object not disposed" errors on the application exit
     */
    override fun dispose() {
        ctx.dispose()
        super.dispose()
    }
}
