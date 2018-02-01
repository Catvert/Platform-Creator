package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.components.RequiredComponent
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant de changer l'atlas en cour d'un gameObject
 */
@RequiredComponent(AtlasComponent::class)
@Description("Permet de changer l'atlas actuel d'un game object")
class AtlasAction(var atlasIndex: Int) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(-1)

    override fun invoke(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<AtlasComponent>()?.also {
            it.currentIndex = atlasIndex
        }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            val atlasData = gameObject.getCurrentState().getComponent<AtlasComponent>()?.data ?: arrayListOf()

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("atlas", ::atlasIndex, atlasData.map { it.name })
            }
        }
    }

    override fun toString(): String = super.toString() + " - index : $atlasIndex"
}