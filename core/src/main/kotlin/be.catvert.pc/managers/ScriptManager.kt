package be.catvert.pc.managers

import be.catvert.pc.actions.Actions
import be.catvert.pc.components.Components
import com.badlogic.gdx.files.FileHandle
import jdk.nashorn.api.scripting.NashornScriptEngine
import javax.script.ScriptContext
import javax.script.ScriptEngineManager

object ScriptManager {
    val scriptEngine = ScriptEngineManager().getEngineByName("nashorn") as NashornScriptEngine

    val bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).apply {
        putAll(Components.values().map { (it.component.simpleName ?: "undefined") to it.component.java })
        putAll(Actions.values().map { (it.action.simpleName ?: "undefined") to it.action.java })
    }

    fun compile(file: FileHandle) = scriptEngine.compile(file.reader())

    fun invokeFunction(name: String, vararg params: Any) {
        scriptEngine.invokeFunction(name, *params)
    }
}