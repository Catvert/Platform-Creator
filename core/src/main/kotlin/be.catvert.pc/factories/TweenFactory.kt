package be.catvert.pc.factories


import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.tweens.AlphaAtlasTween
import be.catvert.pc.tweens.EmptyTween
import be.catvert.pc.tweens.Tween
import be.catvert.pc.tweens.TweenBuilder

enum class TweenFactory(val tween: () -> Tween) {
    Empty({ EmptyTween() }),
    RemoveGO({ TweenBuilder(AlphaAtlasTween(0.5f, 0f), true).build(RemoveGOAction()) });

    operator fun invoke() = tween()
}