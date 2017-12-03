package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.CustomEditorImpl
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

class AtlasAction(var atlasIndex: Int) : Action, CustomEditorImpl {
    @JsonCreator private constructor() : this(-1)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<AtlasComponent>()?.also {
            it.currentIndex = atlasIndex
        }
    }

    override fun insertImgui(labelName: String, gameObject: GameObject, level: Level) {
        with(ImGui) {
            val atlasData = gameObject.getCurrentState().getComponent<AtlasComponent>()?.data ?: arrayListOf()

            functionalProgramming.withItemWidth(100f) {
                combo("atlas", ::atlasIndex, atlasData.map { it.name })
            }
        }
    }
}