package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.eca.Tags
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.*
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui

/**
 * Permet d'effectuer une action sur toutes les entités ayant un tag précis présents dans l'active rect
 */
@Description("[Expérimental] Permet d'effectuer une action sur toutes les entités ayant un tag précis")
class TagAction(@UI(customType = CustomType.TAG_STRING) val tag: EntityTag, var action: Action) : Action(), UIImpl {
    @JsonCreator private constructor() : this(Tags.Player.tag, EmptyAction())

    override fun invoke(entity: Entity) {
        entity.container.cast<Level>()?.findEntitiesByTag(tag)?.forEach {
            action(it)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        entity.container?.findEntitiesByTag(tag)?.firstOrNull()?.apply {
            ImGuiHelper.action("action", ::action, this, level, editorSceneUI)
        } ?: PrefabFactory.values().firstOrNull { it.prefab.prefabEntity.tag == tag }?.apply {
            ImGuiHelper.action("action", ::action, this.prefab.prefabEntity, level, editorSceneUI)
        } ?: ImGui.text("Aucune entité ou prefab\navec ce tag trouvé.")
    }

    override fun toString() = super.toString() + " - tag : $tag | action : $action"
}