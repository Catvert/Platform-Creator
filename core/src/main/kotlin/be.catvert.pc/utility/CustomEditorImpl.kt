package be.catvert.pc.utility

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level

interface CustomEditorImpl {
    fun insertImgui(labelName: String, gameObject: GameObject, level: Level)

    fun insertImguiPopup(gameObject: GameObject, level: Level) = Unit
}