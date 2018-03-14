package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityID
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UI
import be.catvert.pc.ui.UIImpl
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet d'exécuter une action sur une entité précise")
class EntityAction(@UI var target: EntityID, var action: Action) : Action(), UIImpl {
    @JsonCreator private constructor() : this(EntityID(), EmptyAction())

    override fun invoke(entity: Entity, container: EntityContainer) {
        target.entity(container)?.apply {
            action(this, container)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        val target = target.entity(level)
        if (target != null)
            ImGuiHelper.action("target action", ::action, target, level, editorSceneUI)
    }
}