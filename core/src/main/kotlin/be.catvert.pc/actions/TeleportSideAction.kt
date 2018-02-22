package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.builders.TweenBuilder
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.tweens.MoveTween
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de téléporter une entité sur un des côtés d'une autre entité (centré au centre)
 */
@Description("Permet de téléporter une entité sur un des côtés d'une autre entité")
class TeleportSideAction(var teleportTo: GameObject?, @ExposeEditor var spawnSide: BoxSide, @ExposeEditor var tweenMove: Boolean) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(null, BoxSide.Up, true)

    override fun invoke(gameObject: GameObject) {
        if(teleportTo == null)
            return

        val teleportTo = teleportTo!!

        gameObject.container?.apply {
            fun centerX() {
                gameObject.box.x = teleportTo.box.center().x - gameObject.box.width / 2
            }

            fun centerY() {
                gameObject.box.y = teleportTo.box.center().y - gameObject.box.height / 2
            }

            if (tweenMove) {
                var moveX = 0
                var moveY = 0

                when (spawnSide) {
                    BoxSide.Left -> {
                        moveX = -gameObject.box.width
                        gameObject.box.x = teleportTo.box.x
                        centerY()
                    }
                    BoxSide.Right -> {
                        moveX = gameObject.box.width
                        gameObject.box.x = teleportTo.box.right() - gameObject.box.width
                        centerY()
                    }
                    BoxSide.Up -> {
                        moveY = gameObject.box.height
                        gameObject.box.y = teleportTo.box.top() - gameObject.box.height
                        centerX()
                    }
                    BoxSide.Down -> {
                        moveY = -gameObject.box.height
                        gameObject.box.y = teleportTo.box.y
                        centerX()
                    }
                    BoxSide.All -> {
                    }
                }

                TweenAction(TweenBuilder(MoveTween(0.5f, moveX, moveY), true).build(), false).invoke(gameObject)
            } else {
                when (spawnSide) {
                    BoxSide.Left -> {
                        gameObject.box.x = teleportTo.box.x - gameObject.box.width
                        centerY()
                    }
                    BoxSide.Right -> {
                        gameObject.box.x = teleportTo.box.right()
                        centerY()
                    }
                    BoxSide.Up -> {
                        gameObject.box.y = teleportTo.box.top()
                        centerX()
                    }
                    BoxSide.Down -> {
                        gameObject.box.y = teleportTo.box.y - gameObject.box.height
                        centerX()
                    }
                    BoxSide.All -> {
                    }
                }
            }
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.gameObject(::teleportTo, level, editorSceneUI)
    }
}