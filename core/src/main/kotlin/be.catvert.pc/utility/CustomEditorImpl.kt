package be.catvert.pc.utility

import be.catvert.pc.GameObject
import be.catvert.pc.scenes.EditorScene

interface CustomEditorImpl {
    fun insertImgui(gameObject: GameObject, labelName: String, editorScene: EditorScene)

    fun insertImguiPopup(gameObject: GameObject, editorScene: EditorScene) = Unit
}