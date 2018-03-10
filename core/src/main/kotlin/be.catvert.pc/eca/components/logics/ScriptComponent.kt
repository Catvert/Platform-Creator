package be.catvert.pc.eca.components.logics

import be.catvert.pc.Log
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.managers.ScriptsManager
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming
import javax.script.ScriptException

class ScriptComponent(var scriptIndex: Int) : Component(), Updeatable, UIImpl {
    @JsonCreator private constructor() : this(0)

    private var firstCall = true

    private fun executeFunction(name: String, vararg params: Any) {
        entity.container.cast<Level>()?.apply {
            resourcesScripts().elementAtOrNull(scriptIndex)?.apply {
                compiledScript.eval(ScriptsManager.bindings)

                try {
                    ScriptsManager.invokeFunction(name, *params)
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
            executeFunction("init", entity)
            firstCall = false
        }

        executeFunction("update", entity)
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.combo("script", ::scriptIndex, level.resourcesScripts().map { it.file.nameWithoutExtension() })
        }
    }
}