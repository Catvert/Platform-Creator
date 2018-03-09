package be.catvert.pc.eca.actions

import be.catvert.pc.builders.TweenBuilder
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.Prefab
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.tweens.MoveTween
import be.catvert.pc.utility.BoxSide
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet de faire apparaître une entité sur un côté de cette entité")
class SpawnSideAction(@UI var prefab: Prefab, @UI var spawnSide: BoxSide, @UI var tweenMove: Boolean) : Action() {
    @JsonCreator private constructor() : this(PrefabFactory.MushroomRed_SMC.prefab, BoxSide.Up, true)

    override fun invoke(entity: Entity) {
        entity.container?.apply {
            val go = prefab.create(entity.box.position, this)

            fun centerX() {
                go.box.x = entity.box.x + entity.box.width / 2 - go.box.width / 2
            }

            fun centerY() {
                go.box.y = entity.box.y + entity.box.height / 2 - go.box.height / 2
            }

            if (tweenMove) {
                var moveX = 0
                var moveY = 0

                when (spawnSide) {
                    BoxSide.Left -> {
                        moveX = -go.box.width
                        go.box.x = entity.box.x
                        centerY()
                    }
                    BoxSide.Right -> {
                        moveX = go.box.width
                        go.box.x = entity.box.right() - go.box.width
                        centerY()
                    }
                    BoxSide.Up -> {
                        moveY = go.box.height
                        go.box.y = entity.box.top() - go.box.height
                        centerX()
                    }
                    BoxSide.Down -> {
                        moveY = -go.box.height
                        go.box.y = entity.box.y
                        centerX()
                    }
                    BoxSide.All -> {
                    }
                }

                TweenAction(TweenBuilder(MoveTween(0.5f, moveX, moveY), true).build(), false).invoke(go)
            } else {
                when (spawnSide) {
                    BoxSide.Left -> {
                        go.box.x = entity.box.x - go.box.width
                        centerY()
                    }
                    BoxSide.Right -> {
                        go.box.x = entity.box.right()
                        centerY()
                    }
                    BoxSide.Up -> {
                        go.box.y = entity.box.top()
                        centerX()
                    }
                    BoxSide.Down -> {
                        go.box.y = entity.box.y - go.box.height
                        centerX()
                    }
                    BoxSide.All -> {
                    }
                }
            }
        }
    }

    override fun toString() = super.toString() + " - { prefab : $prefab ; side : $spawnSide }"
}