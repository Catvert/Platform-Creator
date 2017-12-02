package be.catvert.pc.factories

import be.catvert.pc.GameObjectTweenAccessor
import be.catvert.pc.actions.EmptyAction
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.TweenComponent
import be.catvert.pc.components.graphics.*
import be.catvert.pc.components.*
import be.catvert.pc.components.logics.*

enum class TweenFactory(val tween: () -> TweenComponent.TweenData) {
    EmptyTween({ TweenComponent.TweenData("empty tween", GameObjectTweenAccessor.GameObjectTween.NOTHING, floatArrayOf(0f), 0f, arrayOf(), EmptyAction()) }),
    RemoveGOTween({ TweenComponent.TweenData("remove go tween", GameObjectTweenAccessor.GameObjectTween.SIZE_Y, floatArrayOf(0f), 0.5f, arrayOf(AtlasComponent::class.java), RemoveGOAction()) });

    operator fun invoke() = tween()
}