package be.catvert.pc.ui

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene

/**
 * Permet à un objet de définir manuellement l'interface graphique générée pour lui.
 */
interface UIImpl {
    fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI)
}