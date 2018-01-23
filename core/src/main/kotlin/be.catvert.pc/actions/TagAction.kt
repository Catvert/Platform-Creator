package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectTag
import be.catvert.pc.Tags
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator

class TagAction(@ExposeEditor(customType = CustomType.TAG_STRING) val tag: GameObjectTag, var action: Action) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(Tags.Player.tag, EmptyAction())

    override fun invoke(gameObject: GameObject) {
        gameObject.container.cast<Level>()?.findGameObjectsByTag(tag)?.forEach {
            action(it)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        gameObject.container.cast<Level>()?.findGameObjectsByTag(tag)?.firstOrNull()?.apply {
            ImguiHelper.action("action", ::action, this, level, editorSceneUI)
        }
    }

    override fun toString() = super.toString() + " - tag : $tag | action : $action"
}