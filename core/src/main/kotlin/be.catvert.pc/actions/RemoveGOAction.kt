package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import com.sun.org.glassfish.gmbal.Description
import imgui.ImGui

/**
 * Action permettant de supprimer une entité
 */
@Description("Permet de supprimer une entité")
class RemoveGOAction : Action(), CustomEditorImpl {
    override fun invoke(gameObject: GameObject) {
        gameObject.removeFromParent()
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGui.text("Il n'y a rien à configurer..")
    }
}