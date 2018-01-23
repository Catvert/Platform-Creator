package be.catvert.pc.utility

import com.badlogic.gdx.InputProcessor

object KeyDownSignalProcessor : InputProcessor {
    val keyDownSignal = Signal<Int>()

    override fun mouseMoved(screenX: Int, screenY: Int) = false
    override fun keyTyped(character: Char) = false
    override fun scrolled(amount: Int) = false
    override fun keyUp(keycode: Int) = false
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    override fun keyDown(keycode: Int): Boolean {
        keyDownSignal(keycode)
        return false
    }
}