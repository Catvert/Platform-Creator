package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.Prefab
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UI
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.Point
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de faire apparaître une entité à un position spécifique
 */
@Description("Permet de faire apparaître une entité à une position spécifique")
class SpawnPositionAction(@UI var prefab: Prefab, var position: Point) : Action(), UIImpl {
    @JsonCreator private constructor() : this(PrefabFactory.MushroomRed_SMC.prefab, Point(0f, 0f))

    override fun invoke(entity: Entity) {
        entity.container?.apply {
            prefab.create(position, this)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        entity.container.cast<Level>()?.matrixRect?.apply {
            ImGuiHelper.point(this@SpawnPositionAction::position, Point(0f, 0f), Point(this.width - entity.size().width.toFloat(), this.height - entity.size().height.toFloat()), editorSceneUI)
        }
    }

    override fun toString() = super.toString() + " - { prefab : $prefab ; position : $position }"
}