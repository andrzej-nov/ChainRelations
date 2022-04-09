package com.andrzejn.chainrelations

import aurelienribon.tweenengine.TweenManager
import com.andrzejn.chainrelations.logic.WorldConstants
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import ktx.assets.Asset
import ktx.assets.loadOnDemand
import space.earlygrey.shapedrawer.ShapeDrawer

/**
 * Holds all application-wide objects.
 * Singleton objects cause a lot of issues on Android because of its memory allocation/release strategy,
 * so everything should be passed in the Context object on each app object creation or method call
 * where it is needed.
 */
class Context(
    /**
     * Reference to the Main game object. Needed to switch game screens on different points of execution.
     */
    val game: Main
) {
    /**
     * The batch for drawing all screen contents.
     */
    lateinit var batch: PolygonSpriteBatch

    /**
     * Camera for drawing all screens. Simple unmoving orthographic camera for static 2D view.
     */
    lateinit var camera: OrthographicCamera

    /**
     * Drawer for geometric shapes on the screens
     */
    lateinit var sd: ShapeDrawer

    /**
     * The main object that handles all animations
     */
    val tweenManager: TweenManager = TweenManager()

    lateinit var wc: WorldConstants

    init { // Need to specify which objects' properties will be used for animations
        //Tween.registerAccessor(Sprite::class.java, SpriteAccessor())
    }

    /**
     * Not clearly documented but working method to check whether some transition animations are in progress
     * (and ignore user input until animations complete, for example)
     */
    fun tweenAnimationRunning(): Boolean {
        return tweenManager.objects.isNotEmpty()
    }

    /**
     * Many times we'll need to fit a sprite into arbitrary rectangle, retaining proportions
     */
    fun fitToRect(s: Sprite, wBound: Float, hBound: Float) {
        var width = wBound
        var height = wBound * s.regionHeight / s.regionWidth
        if (height > hBound) {
            height = hBound
            width = hBound * s.regionWidth / s.regionHeight
        }
        s.setSize(width, height)
    }

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL scene coordinates
     * which are used for drawing
     */
    fun pointerPosition(screenX: Int, screenY: Int): Vector2 {
        val v = Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
        camera.unproject(v)
        return Vector2(v.x, v.y)
    }

    /**
     * Shortened accessor to the screen viewportWidth
     */
    val viewportWidth: Float get() = camera.viewportWidth

    /**
     * Shortened accessor to the screen viewportHeight
     */
    val viewportHeight: Float get() = camera.viewportHeight

    /**
     * Initialize the camera, batch and drawer that draw screens
     */
    fun initBatch() {
        if (this::batch.isInitialized) // Check if the lateinit property has been initialised already
            batch.dispose()
        batch = PolygonSpriteBatch()
        camera = OrthographicCamera()
        setCamera(Gdx.graphics.width, Gdx.graphics.height)
        sd = ShapeDrawer(batch, white) // A single-pixel texture provides the base color.
        // Then actual colors are specified on the drawing methon calls.
    }

    fun setCamera(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()
        batch.projectionMatrix = camera.combined
    }

    private lateinit var atlas: Asset<TextureAtlas>

    /**
     * (Re)load the texture resources definition. In this application we have all textures in the single small PNG
     * picture, so there is just one asset loaded, and loaded synchronously (it is simpler, and does not slow down
     * app startup noticeably)
     */
    fun reloadAtlas() {
        atlas = AssetManager().loadOnDemand("Main.atlas", TextureAtlasLoader.TextureAtlasParameter(false))
    }

    /**
     * Returns reference to particular texture region (sprite image) from the PNG image
     */
    private fun texture(regionName: String): TextureRegion = atlas.asset.findRegion(regionName)

    val white: TextureRegion get() = texture("white")
    val ball: TextureRegion get() = texture("ball")

    /**
     * Create a bitmap font with given size, base color etc. from the provided TrueType font.
     * It is more convenient than keep a lot of fixed font bitmaps for different resolutions.
     */
    fun createFont(height: Int): BitmapFont {
        with(FreeTypeFontGenerator(Gdx.files.internal("ADYS-Bold_V5.ttf"))) {
            val font = generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().also {
                it.size = height
                it.color = Color.WHITE
            })
            dispose()
            return font
        } // don't forget to dispose the font later to avoid memory leaks!
    }

    /**
     * Light (bright) colors palette for the tile lines
     */
    val light: Array<Color> = arrayOf(
        Color.WHITE,
        Color(0xd1b153ff.toInt()),
        Color(0x6d9edbff),
        Color(0x8ab775ff.toInt()),
        Color(0xc260ffff.toInt()),
        Color(0xd68a00ff.toInt()),
        Color(0xdc4125ff.toInt()),
        Color(0x20bfbfff),
    )

    /**
     * Darker colors palette for the tile lines' edges
     */
    val dark: Array<Color> = arrayOf(
        Color.LIGHT_GRAY,
        Color(0xbf9000ff.toInt()),
        Color(0x2265bcff),
        Color(0x38761dff),
        Color(0x56007fff),
        Color(0xa85706ff.toInt()),
        Color(0x85200cff.toInt()),
        Color(0x286d6dff)
    )


    /**
     * Cleanup
     */
    fun dispose() {
        if (this::batch.isInitialized)
            batch.dispose()
    }

}