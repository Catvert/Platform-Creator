package be.catvert.pc.tweens

import be.catvert.pc.GameObject
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.utility.Updeatable

object TweenSystem : Updeatable {
    private data class TweenData(val tween: Tween, val stateBackupIndex: Int)

    private data class TweenInfo(val gameObject: GameObject, val tweenData: TweenData, val loopTween: Tween?)

    private val tweens = mutableListOf<TweenInfo>()

    fun startTween(tween: Tween, gameObject: GameObject, loopTween: Tween?) {
        tweens.add(TweenInfo(gameObject, TweenData(tween, gameObject.getCurrentStateIndex()), loopTween))
        tween.init(gameObject)

        if (tween.useTweenState) {
            val atlas = gameObject.getCurrentState().getComponent<AtlasComponent>()
            val state = gameObject.addState("tween-state") {
                if (atlas != null)
                    addComponent(atlas)
            }
            gameObject.setState(state, false)
        }
    }

    override fun update() {
        val list = mutableListOf(*tweens.toTypedArray())

        list.forEach {
            val gameObject = it.gameObject
            val (tween, backupStateIndex) = it.tweenData
            val loopTween = it.loopTween

            if (tween.update(gameObject)) {
                if (tween.endAction !is RemoveGOAction && tween.useTweenState) // TODO workaround?
                    gameObject.setState(backupStateIndex, false)

                tween.endAction.invoke(gameObject)

                val nextTween = tween.nextTween

                tweens.remove(it)

                if (nextTween != null) {
                    if (tween.useTweenState)
                        gameObject.setState(backupStateIndex, false)

                    startTween(nextTween, gameObject, null)
                } else if (loopTween != null) {
                    startTween(loopTween, gameObject, loopTween)
                }
            }
        }
    }
}