package be.catvert.pc.factories

import be.catvert.pc.GameObjectTweenAccessor
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.TweenComponent

enum class TweenFactory(val tween:() -> TweenComponent.TweenData) {
    EmptyTween({TweenComponent.TweenData("empty tween", GameObjectTweenAccessor.GameObjectTween.NOTHING, floatArrayOf(), 0f, arrayOf(), arrayOf(), EmptyAction())}),
    RemoveGOTween({TweenComponent.TweenData("remove tween", GameObjectTweenAccessor.GameObjectTween.SIZE_Y, floatArrayOf(0f), 0.5f, arrayOf(), arrayOf(), RemoveGOAction())});

    operator fun invoke() = tween()
}