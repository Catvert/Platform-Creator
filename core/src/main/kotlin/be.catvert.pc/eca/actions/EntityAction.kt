package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ImGuiHelper
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet d'exécuter une action sur une entité précise")
class EntityAction(var target: Entity?, var action: Action) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(null, EmptyAction())

    override fun invoke(entity: Entity) {
        target?.apply {
            action(this)
        }
    }

    override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.entity(::target, level, editorSceneUI)
        if (target != null)
            ImGuiHelper.action("target action", ::action, target!!, level, editorSceneUI)
    }
}