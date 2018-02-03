package be.catvert.pc.factories


import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.tweens.*

enum class TweenFactory(val tween: () -> Tween) {
    Empty({ EmptyTween() }),
    RemoveGO({ TweenBuilder(AlphaAtlasTween(0.5f, 0f), true).build(RemoveGOAction()) }),
    ReduceSize({ TweenBuilder(ResizeTween(0.5f), true).build() });

    operator fun invoke() = tween()
}