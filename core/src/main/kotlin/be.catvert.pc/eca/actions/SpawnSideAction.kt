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
        val spawnEntity = prefab.create(entity.box.position, container)

        fun centerX() {
            spawnEntity.box.x = entity.box.x + entity.box.width / 2 - spawnEntity.box.width / 2
        }

        fun centerY() {
            spawnEntity.box.y = entity.box.y + entity.box.height / 2 - spawnEntity.box.height / 2
        }

        if (tweenMove) {
            var moveX = 0
            var moveY = 0

            when (spawnSide) {
                BoxSide.Left -> {
                    moveX = -spawnEntity.box.width
                    spawnEntity.box.x = entity.box.x
                    centerY()
                }
                BoxSide.Right -> {
                    moveX = spawnEntity.box.width
                    spawnEntity.box.x = entity.box.right() - spawnEntity.box.width
                    centerY()
                }
                BoxSide.Up -> {
                    moveY = spawnEntity.box.height
                    spawnEntity.box.y = entity.box.top() - spawnEntity.box.height
                    centerX()
                }
                BoxSide.Down -> {
                    moveY = -spawnEntity.box.height
                    spawnEntity.box.y = entity.box.y
                    centerX()
                }
                BoxSide.All -> {
                }
            }

            TweenAction(TweenBuilder(MoveTween(0.5f, moveX, moveY), true).build(), false).invoke(spawnEntity, container)
        } else {
            when (spawnSide) {
                BoxSide.Left -> {
                    spawnEntity.box.x = entity.box.x - spawnEntity.box.width
                    centerY()
                }
                BoxSide.Right -> {
                    spawnEntity.box.x = entity.box.right()
                    centerY()
                }
                BoxSide.Up -> {
                    spawnEntity.box.y = entity.box.top()
                    centerX()
                }
                BoxSide.Down -> {
                    spawnEntity.box.y = entity.box.y - spawnEntity.box.height
                    centerX()
                }
                BoxSide.All -> {
                }
            }
        }
    }

    override fun toString() = super.toString() + " - { prefab : $prefab ; side : $spawnSide }"
}