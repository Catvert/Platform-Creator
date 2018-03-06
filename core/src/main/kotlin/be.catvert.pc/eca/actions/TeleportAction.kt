package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ImGuiHelper
import be.catvert.pc.utility.Point
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de téléporter un entity à un point précis
 */
@Description("Permet de téléporter une entité")
class TeleportAction(var teleportPoint: Point) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(Point())

    override fun invoke(entity: Entity) {
        entity.box.position = teleportPoint
    }

    override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.point(::teleportPoint, Point(), Point(level.matrixRect.width.toFloat(), level.matrixRect.height.toFloat()), editorSceneUI)
    }

    override fun toString() = super.toString() + " - $teleportPoint"
}