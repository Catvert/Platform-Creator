package be.catvert.pc.tweens

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.actions.RemoveGOAction
import be.catvert.pc.eca.components.graphics.AtlasComponent
import be.catvert.pc.utility.Updeatable

object TweenSystem : Updeatable {
    private data class TweenData(val tween: Tween, val stateBackupIndex: Int)

    private data class TweenInfo(val entity: Entity, val tweenData: TweenData, val loopTween: Tween?)

    private val tweens = mutableListOf<TweenInfo>()

    fun startTween(tween: Tween, entity: Entity, loopTween: Tween?) {
        tweens.add(TweenInfo(entity, TweenData(tween, entity.getCurrentStateIndex()), loopTween))
        tween.init(entity)

        if (tween.useTweenState) {
            val atlas = entity.getCurrentState().getComponent<AtlasComponent>()
            val state = entity.addState("tween-state") {
                if (atlas != null)
                    addComponent(atlas)
            }
            entity.setState(state, false)
        }
    }

    override fun update() {
        val list = mutableListOf(*tweens.toTypedArray())

        list.forEach {
            val entity = it.entity
            val (tween, backupStateIndex) = it.tweenData
            val loopTween = it.loopTween

            if (tween.update(entity)) {
                if (tween.endAction !is RemoveGOAction && tween.useTweenState) // TODO workaround?
                    entity.setState(backupStateIndex, false)

                tween.endAction.invoke(entity)

                val nextTween = tween.nextTween

                tweens.remove(it)

                if (nextTween != null) {
                    if (tween.useTweenState)
                        entity.setState(backupStateIndex, false)

                    startTween(nextTween, entity, null)
                } else if (loopTween != null) {
                    startTween(loopTween, entity, loopTween)
                }
            }
        }
    }
}