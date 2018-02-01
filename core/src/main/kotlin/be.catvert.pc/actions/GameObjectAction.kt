package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.ImguiHelper
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet d'exécuter une action sur un game object précis")
class GameObjectAction(var target: GameObject?, var action: Action) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(null, EmptyAction())

    override fun invoke(gameObject: GameObject) {
        target?.apply {
            action(this)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImguiHelper.gameObject(::target, editorSceneUI)
        if(target != null)
            ImguiHelper.action("target action", ::action, target!!, level, editorSceneUI)
    }
}