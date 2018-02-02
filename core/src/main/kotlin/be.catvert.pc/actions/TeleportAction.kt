package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ImguiHelper
import be.catvert.pc.utility.Point
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de téléporter un gameObject à un point précis
 */
@Description("Permet de téléporter un game object")
class TeleportAction(var teleportPoint: Point) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(Point())

    override fun invoke(gameObject: GameObject) {
        gameObject.box.position = teleportPoint
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImguiHelper.point(::teleportPoint, Point(), Point(level.matrixRect.width.toFloat(), level.matrixRect.height.toFloat()), editorSceneUI)
    }

    override fun toString() = super.toString() + " - $teleportPoint"
}