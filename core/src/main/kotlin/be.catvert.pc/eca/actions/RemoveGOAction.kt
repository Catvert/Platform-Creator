package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.UIImpl
import com.sun.org.glassfish.gmbal.Description
import imgui.ImGui

/**
 * Action permettant de supprimer une entité
 */
@Description("Permet de supprimer une entité")
class RemoveGOAction : Action(), UIImpl {
    override fun invoke(entity: Entity) {
        entity.removeFromParent()
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGui.text("Il n'y a rien à configurer..")
    }
}