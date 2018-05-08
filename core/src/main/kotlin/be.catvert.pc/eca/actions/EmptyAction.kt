package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UIImpl
import imgui.ImGui

@Description("Une action qui ne fait absolument rien")
class EmptyAction : Action(), UIImpl {
    override fun invoke(entity: Entity, container: EntityContainer) {}

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        ImGui.text("Il n'y a rien à faire par ici..")
    }
}