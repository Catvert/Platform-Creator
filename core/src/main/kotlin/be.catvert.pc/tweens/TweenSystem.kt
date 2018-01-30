package be.catvert.pc.tweens

import be.catvert.pc.GameObject
import be.catvert.pc.actions.RemoveGOAction
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.utility.Updeatable

object TweenSystem : Updeatable {
    private data class TweenData(val tween: Tween, val stateBackupIndex: Int)

    private val tweens = mutableListOf<Pair<GameObject, TweenData>>()

    fun startTween(tween: Tween, gameObject: GameObject) {
        tweens.add(gameObject to TweenData(tween, gameObject.getCurrentStateIndex()))
        tween.init(gameObject)

        if (tween.useTweenState) {
            val atlas = gameObject.getCurrentState().getComponent<AtlasComponent>()
            val state = gameObject.addState("tween-state") {
                if (atlas != null)
                    addComponent(atlas)
            }
            gameObject.setState(state)
        }
    }

    override fun update() {
        val it = tweens.iterator()
        while (it.hasNext()) {
            val keyValue = it.next()

            val gameObject = keyValue.first
            val (tween, backupStateIndex) = keyValue.second

            if (tween.update(gameObject)) {
                if (tween.endAction !is RemoveGOAction) // TODO workaround?
                    gameObject.setState(backupStateIndex)

                tween.endAction.invoke(gameObject)

                val nextTween = tween.nextTween

                if (nextTween != null) {
                    gameObject.setState(backupStateIndex)
                    startTween(nextTween, gameObject)
                } else {
                    it.remove()
                }
            }
        }
    }
}