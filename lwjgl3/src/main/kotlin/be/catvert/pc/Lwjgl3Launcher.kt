package be.catvert.pc

import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.GameConfig
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

/** Launches the desktop (LWJGL3) application.  */
object Lwjgl3Launcher {
    private val configuration: Pair<GameConfig, Lwjgl3ApplicationConfiguration>
        get() {
            val configuration = Lwjgl3ApplicationConfiguration()

            configuration.setTitle(Constants.gameTitle)

            val config = GameConfig.loadGameConfig()

            if(config.fullScreen) {
                val mode = Lwjgl3ApplicationConfiguration.getDisplayModes().firstOrNull { it.width == config.screenWidth && it.height == config.screenHeight && it.refreshRate == config.refreshRate } ?:
                Lwjgl3ApplicationConfiguration.getDisplayMode()
                configuration.setFullscreenMode(mode)
            }
            else
                configuration.setWindowedMode(config.screenWidth, config.screenHeight)

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