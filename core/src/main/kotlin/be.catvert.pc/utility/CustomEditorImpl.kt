package be.catvert.pc.utility

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level

interface CustomEditorImpl {
    fun insertImgui(label: String, gameObject: GameObject, level: Level)
}