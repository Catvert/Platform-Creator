package be.catvert.pc.ui

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene

interface UIImpl {
    fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI)
}