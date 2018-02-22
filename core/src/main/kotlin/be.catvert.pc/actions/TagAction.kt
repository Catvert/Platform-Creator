package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTag
import be.catvert.pc.Tags
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui

/**
 * Permet d'effectuer une action sur toutes les entités ayant un tag précis présents dans l'active rect
 */
@Description("[Expérimental] Permet d'effectuer une action sur toutes les entités ayant un tag précis")
class TagAction(@ExposeEditor(customType = CustomType.TAG_STRING) val tag: GameObjectTag, var action: Action) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(Tags.Player.tag, EmptyAction())

    override fun invoke(gameObject: GameObject) {
        gameObject.container.cast<Level>()?.findGameObjectsByTag(tag)?.forEach {
            action(it)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        gameObject.container?.findGameObjectsByTag(tag)?.firstOrNull()?.apply {
            ImGuiHelper.action("action", ::action, this, level, editorSceneUI)
        } ?: PrefabFactory.values().firstOrNull { it.prefab.prefabGO.tag == tag }?.apply {
            ImGuiHelper.action("action", ::action, this.prefab.prefabGO, level, editorSceneUI)
        } ?: ImGui.text("Aucune entité ou prefab\navec ce tag trouvé.")
    }

    override fun toString() = super.toString() + " - tag : $tag | action : $action"
}