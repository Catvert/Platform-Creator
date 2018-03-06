package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.graphics.AtlasComponent
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.CustomEditorImpl
import be.catvert.pc.utility.Description
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant de changer l'atlas en cour d'une entité
 */
@RequiredComponent(AtlasComponent::class)
@Description("Permet de changer l'atlas actuel d'une entité")
class AtlasAction(var atlasIndex: Int) : Action(), CustomEditorImpl {
    @JsonCreator private constructor() : this(-1)

    override fun invoke(entity: Entity) {
        entity.getCurrentState().getComponent<AtlasComponent>()?.also {
            it.currentIndex = atlasIndex
        }
    }

    override fun insertImgui(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            val atlasData = entity.getCurrentState().getComponent<AtlasComponent>()?.data ?: arrayListOf()

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("atlas", ::atlasIndex, atlasData.map { it.name })
            }
        }
    }

    override fun toString(): String = super.toString() + " - index : $atlasIndex"
}