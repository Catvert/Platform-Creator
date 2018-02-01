package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import imgui.ImGui

/**
 * Une action qui ne fait rien
 */
@Description("Une action qui ne fait absolument rien")
class EmptyAction : Action(), CustomEditorImpl {
    override fun invoke(gameObject: GameObject) {}

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGui.text("Il n'y a rien à faire par ici..")
    }
}