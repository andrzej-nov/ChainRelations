package com.andrzejn.chainrelations

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

/** Launches the desktop (LWJGL3) application.  */
object DesktopLauncher {
    /**
     *
     */
    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(Main(), Lwjgl3ApplicationConfiguration().apply {
            setTitle("Chain Relations")
            setWindowedMode(960, 960)
            setForegroundFPS(60)
            setWindowIcon("icon256.png", "icon128.png", "icon64.png", "icon32.png", "icon16.png")
        })
    }
}