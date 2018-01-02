package be.catvert.pc

import be.catvert.pc.serialization.SerializationFactory
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.GameConfig
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.sun.org.apache.xml.internal.dtm.Axis.getNames
import jogamp.graph.font.typecast.ot.table.ID.getLanguageName
import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import javax.script.ScriptEngineFactory
import javax.script.ScriptEngineManager



/** Launches the desktop (LWJGL3) application.  */
object Lwjgl3Launcher {
    private val configuration: Pair<GameConfig, Lwjgl3ApplicationConfiguration>
        get() {
            val configuration = Lwjgl3ApplicationConfiguration()

            configuration.setTitle(Constants.gameTitle)

            val config = GameConfig.loadGameConfig()

            if(config.fullScreen) {
                val primaryMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
                configuration.setFullscreenMode(primaryMode)
            }
            else
                configuration.setWindowedMode(config.screenWidth, config.screenHeight)

            configuration.setResizable(true)
            configuration.useVsync(true)

            return config to configuration
        }

    class T1(val checked: Boolean, val t2: T2) {
    }

    class T2(val str: String)

    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(PCGame(configuration.first), configuration.second)
    }
}