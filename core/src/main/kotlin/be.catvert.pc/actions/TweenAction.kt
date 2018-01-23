package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.tweens.MoveTween
import be.catvert.pc.tweens.Tween
import be.catvert.pc.tweens.TweenSystem
import com.fasterxml.jackson.annotation.JsonCreator

class TweenAction(var tween: Tween): Action() {
    @JsonCreator private constructor(): this(MoveTween(1f, 100, 0))
    override fun invoke(gameObject: GameObject) {
        TweenSystem.startTween(tween, gameObject)
    }
}