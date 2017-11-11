package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.GameObjectState
import be.catvert.pc.components.SoundComponent
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import ktx.actors.onChange
import ktx.collections.toGdxArray

/**
 * Action permettant de jouer un son à partir d'un gameObject ayant un SoundComponent
 * @see SoundComponent
 */
class SoundAction(var soundIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor(): this(-1)
    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<SoundComponent>()?.playSound(soundIndex)
    }

    override fun insertChangeProperties(table: VisTable, gameObject: GameObject, editorScene: EditorScene) {
        table.add(VisSelectBox<SoundComponent.SoundData>().apply {
            val sounds = gameObject.getCurrentState().getComponent<SoundComponent>()?.sounds

            this.items = sounds?.toGdxArray()?: ktx.collections.gdxArrayOf()

            if(sounds?.indices?.contains(soundIndex) == true)
                selectedIndex = soundIndex

            onChange {
                soundIndex = selectedIndex
            }
        })
    }
}