package be.catvert.pc.eca.actions

import be.catvert.pc.builders.TweenBuilder
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.tweens.MoveTween
import be.catvert.pc.utility.*
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant de téléporter une entité sur un des côtés d'une autre entité (centré au centre)
 */
@Description("Permet de téléporter une entité sur un des côtés d'une autre entité")
class TeleportSideAction(var teleportTo: Entity?, @ExposeEditor var spawnSide: BoxSide, @ExposeEditor var tweenMove: Boolean) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(null, BoxSide.Up, true)

    override fun invoke(entity: Entity) {
        if (teleportTo == null)
            return

        val teleportTo = teleportTo!!

        entity.container?.apply {
            fun centerX() {
                entity.box.x = teleportTo.box.center().x - entity.box.width / 2
            }

            fun centerY() {
                entity.box.y = teleportTo.box.center().y - entity.box.height / 2
            }

            if (tweenMove) {
                var moveX = 0
                var moveY = 0

                when (spawnSide) {
                    BoxSide.Left -> {
                        moveX = -entity.box.width
                        entity.box.x = teleportTo.box.x
                        centerY()
                    }
                    BoxSide.Right -> {
                        moveX = entity.box.width
                        entity.box.x = teleportTo.box.right() - entity.box.width
                        centerY()
                    }
                    BoxSide.Up -> {
                        moveY = entity.box.height
                        entity.box.y = teleportTo.box.top() - entity.box.height
                        centerX()
                    }
                    BoxSide.Down -> {
                        moveY = -entity.box.height
                        entity.box.y = teleportTo.box.y
                        centerX()
                    }
                    BoxSide.All -> {
                    }
                }

                TweenAction(TweenBuilder(MoveTween(0.5f, moveX, moveY), true).build(), false).invoke(entity)
            } else {
                when (spawnSide) {
                    BoxSide.Left -> {
                        entity.box.x = teleportTo.box.x - entity.box.width
                        centerY()
                    }
                    BoxSide.Right -> {
                        entity.box.x = teleportTo.box.right()
                        centerY()
                    }
                    BoxSide.Up -> {
                        entity.box.y = teleportTo.box.top()
                        centerX()
                    }
                    BoxSide.Down -> {
                        entity.box.y = teleportTo.box.y - entity.box.height
                        centerX()
                    }
                    BoxSide.All -> {
                    }
                }
            }
        }
    }

    override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.entity(::teleportTo, level, editorSceneUI)
    }
}