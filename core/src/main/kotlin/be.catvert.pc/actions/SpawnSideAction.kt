package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.tweens.MoveTween
import be.catvert.pc.tweens.TweenBuilder
import be.catvert.pc.utility.BoxSide
import be.catvert.pc.utility.Description
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

@Description("Permet de faire apparaître un game object sur un côté de ce game object")
class SpawnSideAction(@ExposeEditor var prefab: Prefab, @ExposeEditor var spawnSide: BoxSide, @ExposeEditor var tweenMove: Boolean) : Action() {
    @JsonCreator private constructor() : this(PrefabFactory.MushroomRed_SMC.prefab, BoxSide.Left, true)

    override fun invoke(gameObject: GameObject) {
        gameObject.container?.apply {
            val go = prefab.create(gameObject.box.position, this)

            fun centerX() {
                go.box.x = gameObject.box.x + gameObject.box.width / 2 - go.box.width / 2
            }

            fun centerY() {
                go.box.y = gameObject.box.y + gameObject.box.height / 2 - go.box.height / 2
            }

            if (tweenMove) {
                var moveX = 0
                var moveY = 0

                when (spawnSide) {
                    BoxSide.Left -> {
                        moveX = -go.box.width
                        go.box.x = gameObject.box.x
                        centerY()
                    }
                    BoxSide.Right -> {
                        moveX = go.box.width
                        go.box.x = gameObject.box.right() - go.box.width
                        centerY()
                    }
                    BoxSide.Up -> {
                        moveY = go.box.height
                        go.box.y = gameObject.box.top() - go.box.height
                        centerX()
                    }
                    BoxSide.Down -> {
                        moveY = -go.box.height
                        go.box.y = gameObject.box.y
                        centerX()
                    }
                    BoxSide.All -> {
                    }
                }

                TweenAction(TweenBuilder(MoveTween(0.5f, moveX, moveY), true).build(), false).invoke(go)
            } else {
                when (spawnSide) {
                    BoxSide.Left -> {
                        go.box.x = gameObject.box.x - go.box.width
                        centerY()
                    }
                    BoxSide.Right -> {
                        go.box.x = gameObject.box.right()
                        centerY()
                    }
                    BoxSide.Up -> {
                        go.box.y = gameObject.box.top()
                        centerX()
                    }
                    BoxSide.Down -> {
                        go.box.y = gameObject.box.y - go.box.height
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