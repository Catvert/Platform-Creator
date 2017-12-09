package be.catvert.pc

import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Utility
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.sun.org.apache.xml.internal.dtm.Axis.getNames
import jogamp.graph.font.typecast.ot.table.ID.getLanguageName
import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import javax.script.ScriptEngineFactory
import javax.script.ScriptEngineManager



/** Launches the desktop (LWJGL3) application.  */
object Lwjgl3Launcher {
    private val configuration: Pair<Lwjgl3ApplicationConfiguration, Float>
        get() {
            val configuration = Lwjgl3ApplicationConfiguration()
            val (width, height, fullscreen, soundVolume) = Utility.loadGameConfig()

            configuration.setTitle(Constants.gameTitle)
            if(fullscreen) {
                val primaryMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
                configuration.setFullscreenMode(primaryMode)
            }
            else
                configuration.setWindowedMode(width, height)

            configuration.setResizable(false)
            configuration.useVsync(true)

            return configuration to soundVolume
        }

    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(PCGame(configuration.second), configuration.first)
    }
}