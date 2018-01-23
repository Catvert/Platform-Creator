package be.catvert.pc.factories


import be.catvert.pc.tweens.*
import be.catvert.pc.actions.*

enum class TweenFactory(val tween: () -> Tween) {
    EmptyTween({ EmptyTween() }),
    RemoveGOTween({ TweenBuilder(AlphaAtlasTween(0.5f, 0f), true).build(RemoveGOAction()) });

    operator fun invoke() = tween()
}