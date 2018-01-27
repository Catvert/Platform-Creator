package be.catvert.pc.utility

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import glm_.vec2.Vec2d
import imgui.impl.LwjglGL3
import org.lwjgl.glfw.GLFW

object PCInputProcessor : InputAdapter() {
    val keyDownSignal = Signal<Int>()

    override fun keyTyped(character: Char): Boolean {
        LwjglGL3.charCallback(character.toInt())
        return false
    }

    override fun scrolled(amount: Int): Boolean {
        LwjglGL3.scrollCallback(Vec2d(0, -amount))
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        LwjglGL3.mouseButtonCallback(button, GLFW.GLFW_PRESS, 0)
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        fun sendKeyRelease(key: Int) {
            LwjglGL3.keyCallback(key, 0, GLFW.GLFW_RELEASE, 0)
        }

        when (keycode) {
            Input.Keys.BACKSPACE -> sendKeyRelease(GLFW.GLFW_KEY_BACKSPACE)
            Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> sendKeyRelease(GLFW.GLFW_KEY_LEFT_CONTROL)
            Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> sendKeyRelease(GLFW.GLFW_KEY_LEFT_ALT)
            Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> sendKeyRelease(GLFW.GLFW_KEY_LEFT_SHIFT)
            Input.Keys.BACKSLASH -> sendKeyRelease(GLFW.GLFW_KEY_BACKSLASH)
            Input.Keys.LEFT -> sendKeyRelease(GLFW.GLFW_KEY_LEFT)
            Input.Keys.RIGHT -> sendKeyRelease(GLFW.GLFW_KEY_RIGHT)
            Input.Keys.UP -> sendKeyRelease(GLFW.GLFW_KEY_UP)
            Input.Keys.DOWN -> sendKeyRelease(GLFW.GLFW_KEY_DOWN)
            Input.Keys.TAB -> sendKeyRelease(GLFW.GLFW_KEY_TAB)
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        keyDownSignal(keycode)

        fun sendKeyPressed(key: Int) {
            LwjglGL3.keyCallback(key, 0, GLFW.GLFW_PRESS, 0)
        }

        when (keycode) {
            Input.Keys.BACKSPACE -> sendKeyPressed(GLFW.GLFW_KEY_BACKSPACE)
            Input.Keys.BACKSLASH -> sendKeyPressed(GLFW.GLFW_KEY_BACKSLASH)
            Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> sendKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)
            Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> sendKeyPressed(GLFW.GLFW_KEY_LEFT_ALT)
            Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> sendKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)
            Input.Keys.LEFT -> sendKeyPressed(GLFW.GLFW_KEY_LEFT)
            Input.Keys.RIGHT -> sendKeyPressed(GLFW.GLFW_KEY_RIGHT)
            Input.Keys.UP -> sendKeyPressed(GLFW.GLFW_KEY_UP)
            Input.Keys.DOWN -> sendKeyPressed(GLFW.GLFW_KEY_DOWN)
            Input.Keys.TAB -> sendKeyPressed(GLFW.GLFW_KEY_TAB)
        }

        return false
    }
}