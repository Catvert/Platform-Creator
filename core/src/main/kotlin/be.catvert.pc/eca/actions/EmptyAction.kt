package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.ui.Description
import imgui.ImGui

/**
 * Une action qui ne fait rien
 */
@Description("Une action qui ne fait absolument rien")
class EmptyAction : Action(), UIImpl {
    override fun invoke(entity: Entity) {}

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGui.text("Il n'y a rien à faire par ici..")
    }
}