package be.catvert.pc

import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.GameConfig
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import glm_.func.common.clamp
import glm_.min

/** Launches the desktop (LWJGL3) application.  */
object Lwjgl3Launcher {
    private val configuration: Pair<GameConfig, Lwjgl3ApplicationConfiguration>
        get() {
            val configuration = Lwjgl3ApplicationConfiguration()

            configuration.setTitle(Constants.gameTitle)

            val config = GameConfig.loadGameConfig()

            if(config.fullScreen) {
                val mode = Lwjgl3ApplicationConfiguration.getDisplayModes().firstOrNull { it.width == config.screenWidth && it.height == config.screenHeight && it.refreshRate == config.refreshRate } ?: Lwjgl3ApplicationConfiguration.getDisplayMode()
                configuration.setFullscreenMode(mode)
            }
            else {
                val mode = Lwjgl3ApplicationConfiguration.getDisplayMode()
                configuration.setWindowedMode(config.screenWidth.clamp(100, mode.width), config.screenHeight.clamp(100, mode.height))
                configuration.setWindowPosition(mode.width / 2 - config.screenWidth.min(mode.width) / 2, mode.height / 2 - config.screenHeight.min(mode.height) / 2)
            }

            configuration.setResizable(true)
            configuration.useVsync(true)
            configuration.setWindowIcon("assets/icon.png")

            return config to configuration
        }
    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(PCGame(configuration.first), configuration.second)
    }
}