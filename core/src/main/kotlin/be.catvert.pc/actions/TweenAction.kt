package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.systems.TweenSystem2
import com.fasterxml.jackson.annotation.JsonCreator

class TweenAction2(var tween: TweenSystem2.Tween): Action() {
    @JsonCreator private constructor(): this(TweenSystem2.MoveTween(1f, 100, 0))
    override fun invoke(gameObject: GameObject) {
        TweenSystem2.start(tween, gameObject)
    }
}