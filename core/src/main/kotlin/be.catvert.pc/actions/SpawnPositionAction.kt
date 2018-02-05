package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet de faire apparaître un game object à une position spécifique")
class SpawnPositionAction(@ExposeEditor var prefab: Prefab, var position: Point) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(PrefabFactory.MushroomRed_SMC.prefab, Point(0f, 0f))

    override fun invoke(gameObject: GameObject) {
        gameObject.container?.apply {
            prefab.create(position, this)
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        (gameObject.container as? Level)?.matrixRect?.apply {
            ImGuiHelper.point(this@SpawnPositionAction::position, Point(0f, 0f), Point(this.width - gameObject.size().width.toFloat(), this.height - gameObject.size().height.toFloat()), editorSceneUI)
        }
    }

    override fun toString() = super.toString() + " - { prefab : $prefab ; position : $position }"
}