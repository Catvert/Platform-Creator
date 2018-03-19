package be.catvert.pc.managers

import be.catvert.pc.eca.actions.Actions
import be.catvert.pc.eca.components.Components
import com.badlogic.gdx.files.FileHandle
import jdk.nashorn.api.scripting.NashornScriptEngine
import javax.script.ScriptContext
import javax.script.ScriptEngineManager

/**
 * Permet de gérer les scripts
 */
object ScriptsManager {
    val scriptEngine = ScriptEngineManager().getEngineByName("nashorn") as NashornScriptEngine

    /**
     * Permet de donner à javascript l'accès aux components et actions
     */
    val bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).apply {
        putAll(Components.values().map { (it.component.simpleName ?: "undefined") to it.component.java })
        putAll(Actions.values().map { (it.action.simpleName ?: "undefined") to it.action.java })
    }

    /**
     * Permet de compiler un script, dans le but de rendre l'exécution de celui-ci plus rapide
     */
    fun compile(file: FileHandle) = scriptEngine.compile(file.reader())

    /**
     * Permet d'invoquer une fonction javascript
     */
    fun invokeFunction(name: String, vararg params: Any) {
        scriptEngine.invokeFunction(name, *params)
    }
}