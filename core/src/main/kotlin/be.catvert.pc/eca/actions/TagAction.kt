package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.eca.Tags
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.*
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui

/**
 * Permet d'effectuer une action sur toutes les entités ayant un tag précis présents dans l'active rect
 */
@Description("[Expérimental] Permet d'effectuer une action sur toutes les entités ayant un tag précis")
class TagAction(@UI(customType = CustomType.TAG_STRING) var tag: EntityTag, var action: Action) : Action(), UIImpl {
    @JsonCreator private constructor() : this(Tags.Player.tag, EmptyAction())

    override fun invoke(entity: Entity, container: EntityContainer) {
        container.findEntitiesByTag(tag).forEach {
            action(it, container)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        level.findEntitiesByTag(tag).firstOrNull()?.apply {
            ImGuiHelper.action("action", ::action, this, level, editorUI)
        } ?: let {
            val prefab = PrefabFactory.values().map { it.prefab }.firstOrNull { it.prefabEntity.tag == tag }
                    ?: level.resourcesPrefabs().firstOrNull { it.prefabEntity.tag == tag }
            prefab?.apply {
                ImGuiHelper.action("action", ::action, this.prefabEntity, level, editorUI)
            }
        } ?: ImGui.text("Aucune entité ou prefab\navec ce tag trouvé.")
    }

    override fun toString() = super.toString() + " - tag : $tag | action : $action"
}