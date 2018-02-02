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
            gameObject.setState(state, false)
        }
    }

    override fun update() {
        val list = mutableListOf(*tweens.toTypedArray())

        list.forEach {
            val gameObject = it.first
            val (tween, backupStateIndex) = it.second

            if (tween.update(gameObject)) {
                if (tween.endAction !is RemoveGOAction) // TODO workaround?
                    gameObject.setState(backupStateIndex, false)

                tween.endAction.invoke(gameObject)

                val nextTween = tween.nextTween

                tweens.remove(it)

                if (nextTween != null) {
                    gameObject.setState(backupStateIndex, false)

                    startTween(nextTween, gameObject)
                }
            }
        }
    }
}