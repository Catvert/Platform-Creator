package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityChecker
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UI
import be.catvert.pc.ui.UIImpl
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet d'exécuter une action sur une entité précise")
class EntityAction(@UI var target: EntityChecker, var action: Action) : Action(), UIImpl {
    @JsonCreator private constructor() : this(EntityChecker(), EmptyAction())

    override fun invoke(entity: Entity) {
        action(target)
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        if (target.entity != null)
            ImGuiHelper.action("target action", ::action, target.entity!!, level, editorSceneUI)
    }
}