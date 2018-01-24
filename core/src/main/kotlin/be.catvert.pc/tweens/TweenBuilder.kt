package be.catvert.pc.tweens

import be.catvert.pc.actions.Action
import be.catvert.pc.actions.EmptyAction

class TweenBuilder(private val initialTween: Tween, useTweenState: Boolean = false) {
    private var actualTween: Tween = initialTween

    init {
        initialTween.useTweenState = useTweenState
    }

    fun then(tween: Tween, useTweenState: Boolean = false): TweenBuilder {
        tween.useTweenState = useTweenState
        actualTween.nextTween = tween
        actualTween = tween

        return this
    }

    fun build(endAction: Action = EmptyAction()): Tween {
        actualTween.endAction = endAction
        return initialTween
    }
}