package be.catvert.pc.factories

import be.catvert.pc.GameObjectTweenAccessor
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.TweenComponent
import be.catvert.pc.components.graphics.AtlasComponent

enum class TweenFactory(val tween: () -> TweenComponent.TweenData) {
    EmptyTween({ TweenComponent.TweenData("empty tween", GameObjectTweenAccessor.GameObjectTween.NOTHING, floatArrayOf(0f), 0f, arrayListOf(), EmptyAction()) }),
    RemoveGOTween({ TweenComponent.TweenData("remove go tween", GameObjectTweenAccessor.GameObjectTween.SIZE_Y, floatArrayOf(0f), 0.5f, arrayListOf(AtlasComponent::class.java), RemoveGOAction()) });

    operator fun invoke() = tween()
}