package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.ImGuiHelper
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.Point
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet de téléporter une entité")
class TeleportAction(var teleportPoint: Point) : Action(), UIImpl {
    @JsonCreator private constructor() : this(Point())

    override fun invoke(entity: Entity, container: EntityContainer) {
        entity.box.position = teleportPoint
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        ImGuiHelper.point(::teleportPoint, Point(), Point(level.matrixRect.width.toFloat(), level.matrixRect.height.toFloat()), editorUI)
    }

    override fun toString() = super.toString() + " - $teleportPoint"
}