package be.catvert.pc.utility

import be.catvert.pc.scenes.EditorScene
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisTable

interface CustomEditorImpl {
    fun insertChangeProperties(table: VisTable, editorScene: EditorScene)
}