package be.catvert.pc.tweens

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.actions.RemoveEntityAction
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.utility.Updeatable

object TweenSystem : Updeatable {
    private data class TweenData(val tween: Tween, val stateBackupIndex: Int)

    private data class TweenInfo(val entity: Entity, val tweenData: TweenData, val loopTween: Tween?)

    private val tweens = mutableListOf<TweenInfo>()

    fun startTween(tween: Tween, entity: Entity, loopTween: Tween?) {
        tweens.add(TweenInfo(entity, TweenData(tween, entity.getCurrentStateIndex()), loopTween))
        tween.init(entity)

        if (tween.useTweenState) {
            val texture = entity.getCurrentState().getComponent<TextureComponent>()
            val state = entity.addState("tween-state") {
                if (texture != null)
                    addComponent(texture)
            }
            entity.setState(state)
        }
    }

    override fun update() {
        val list = mutableListOf(*tweens.toTypedArray())

        list.forEach {
            val entity = it.entity
            val container = entity.container ?: return@forEach
            val (tween, backupStateIndex) = it.tweenData
            val loopTween = it.loopTween

            if (container.allowUpdating && tween.update(entity)) {
                if (tween.useTweenState)
                    entity.setState(backupStateIndex)

                tween.endAction.invoke(entity, container)

                val nextTween = tween.nextTween

                tweens.remove(it)

                if (nextTween != null) {
                    if (tween.useTweenState)
                        entity.setState(backupStateIndex)

                    startTween(nextTween, entity, loopTween)
                } else if (loopTween != null) {
                    startTween(loopTween, entity, loopTween)
                }
            }
        }
    }
}