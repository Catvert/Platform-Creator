package be.catvert.pc.utility

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene

interface CustomEditorImpl {
    fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI)
}