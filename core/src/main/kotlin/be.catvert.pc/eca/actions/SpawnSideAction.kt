package be.catvert.pc.eca.actions

import be.catvert.pc.builders.TweenBuilder
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.Prefab
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.tweens.MoveTween
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.utility.BoxSide
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet de faire apparaître une entité sur un côté de cette entité")
class SpawnSideAction(@UI var prefab: Prefab, @UI var spawnSide: BoxSide, @UI var tweenMove: Boolean) : Action() {
    @JsonCreator private constructor() : this(PrefabFactory.MushroomRed_SMC.prefab, BoxSide.Up, true)

    override fun invoke(entity: Entity, container: EntityContainer) {
        val entity = prefab.create(entity.box.position, container)

        fun centerX() {
            entity.box.x = entity.box.x + entity.box.width / 2 - entity.box.width / 2
        }

        fun centerY() {
            entity.box.y = entity.box.y + entity.box.height / 2 - entity.box.height / 2
        }

        if (tweenMove) {
            var moveX = 0
            var moveY = 0

            when (spawnSide) {
                BoxSide.Left -> {
                    moveX = -entity.box.width
                    entity.box.x = entity.box.x
                    centerY()
                }
                BoxSide.Right -> {
                    moveX = entity.box.width
                    entity.box.x = entity.box.right() - entity.box.width
                    centerY()
                }
                BoxSide.Up -> {
                    moveY = entity.box.height
                    entity.box.y = entity.box.top() - entity.box.height
                    centerX()
                }
                BoxSide.Down -> {
                    moveY = -entity.box.height
                    entity.box.y = entity.box.y
                    centerX()
                }
                BoxSide.All -> {
                }
            }

            TweenAction(TweenBuilder(MoveTween(0.5f, moveX, moveY), true).build(), false).invoke(entity, container)
        } else {
            when (spawnSide) {
                BoxSide.Left -> {
                    entity.box.x = entity.box.x - entity.box.width
                    centerY()
                }
                BoxSide.Right -> {
                    entity.box.x = entity.box.right()
                    centerY()
                }
                BoxSide.Up -> {
                    entity.box.y = entity.box.top()
                    centerX()
                }
                BoxSide.Down -> {
                    entity.box.y = entity.box.y - entity.box.height
                    centerX()
                }
                BoxSide.All -> {
                }
            }
        }
    }

    override fun toString() = super.toString() + " - { prefab : $prefab ; side : $spawnSide }"
}