package be.catvert.pc.eca

import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.serialization.PostDeserialization
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.SignalListener

class EntityChecker(entity: Entity? = null) : UIImpl, PostDeserialization {
    private var entityRemoveListener: SignalListener<Entity>? = null

    var entity: Entity? = entity
        set(value) {
            field = value
            registerRemoveListener()
        }

    private fun registerRemoveListener() {
        entityRemoveListener?.cancel = true

        entityRemoveListener = entity?.onRemoveFromParent?.register(true) {
            if (entity === it) {
                entity = null
            }
        }
    }

    override fun onPostDeserialization() {
        registerRemoveListener()
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.entity(this, level, editorSceneUI)
    }
}