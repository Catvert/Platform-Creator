package be.catvert.pc.components.logics

import be.catvert.pc.GameObject
import be.catvert.pc.Log
import be.catvert.pc.components.Component
import be.catvert.pc.containers.Level
import be.catvert.pc.managers.ScriptManager
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming
import javax.script.ScriptException

class ScriptComponent(var scriptIndex: Int) : Component(), Updeatable, CustomEditorImpl {
    @JsonCreator private constructor() : this(0)

    private var firstCall = true

    private fun executeFunction(name: String, vararg params: Any) {
        gameObject.container.cast<Level>()?.apply {
            resourcesScripts().elementAtOrNull(scriptIndex)?.apply {
                compiledScript.eval(ScriptManager.bindings)

                try {
                    ScriptManager.invokeFunction(name, *params)
                } catch (e: NoSuchMethodException) {
                    Log.error(e) { "Impossible de trouver la méthode $name dans le script : $file" }
                } catch (e: ScriptException) {
                    Log.error(e) { "Une erreur s'est produite lors de l'exécution du script : $file" }
                }
            }
        }
    }

    override fun update() {
        if (firstCall) {
            executeFunction("init", gameObject)
            firstCall = false
        }

        executeFunction("update", gameObject)
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.combo("script", ::scriptIndex, level.resourcesScripts().map { it.file.nameWithoutExtension() })
        }
    }
}