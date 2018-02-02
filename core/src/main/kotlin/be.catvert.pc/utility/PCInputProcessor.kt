package be.catvert.pc.utility

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import glm_.vec2.Vec2d
import imgui.impl.LwjglGL3
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL

object PCInputProcessor : InputAdapter() {
    val keyDownSignal = Signal<Int>()

    private val gdxGLFWKeyMap = mutableMapOf<Int, Int>()

    init {
        gdxGLFWKeyMap[Input.Keys.TAB] = GLFW.GLFW_KEY_TAB

        gdxGLFWKeyMap[Input.Keys.LEFT] = GLFW.GLFW_KEY_LEFT
        gdxGLFWKeyMap[Input.Keys.RIGHT] = GLFW.GLFW_KEY_RIGHT
        gdxGLFWKeyMap[Input.Keys.UP] = GLFW.GLFW_KEY_UP
        gdxGLFWKeyMap[Input.Keys.DOWN] = GLFW.GLFW_KEY_DOWN

        gdxGLFWKeyMap[Input.Keys.PAGE_UP] = GLFW.GLFW_KEY_PAGE_UP
        gdxGLFWKeyMap[Input.Keys.PAGE_DOWN] = GLFW.GLFW_KEY_PAGE_DOWN

        gdxGLFWKeyMap[Input.Keys.HOME] = GLFW.GLFW_KEY_HOME
        gdxGLFWKeyMap[Input.Keys.END] = GLFW.GLFW_KEY_END

        gdxGLFWKeyMap[Input.Keys.BACKSPACE] = GLFW.GLFW_KEY_BACKSPACE

        gdxGLFWKeyMap[Input.Keys.ENTER] = GLFW.GLFW_KEY_ENTER
        gdxGLFWKeyMap[Input.Keys.ESCAPE] = GLFW.GLFW_KEY_ESCAPE

        gdxGLFWKeyMap[Input.Keys.CONTROL_LEFT] = GLFW.GLFW_KEY_LEFT_CONTROL
        gdxGLFWKeyMap[Input.Keys.CONTROL_RIGHT] = GLFW.GLFW_KEY_RIGHT_CONTROL
        gdxGLFWKeyMap[Input.Keys.ALT_LEFT] = GLFW.GLFW_KEY_LEFT_ALT
        gdxGLFWKeyMap[Input.Keys.ALT_RIGHT] = GLFW.GLFW_KEY_RIGHT_ALT
        gdxGLFWKeyMap[Input.Keys.SHIFT_LEFT] = GLFW.GLFW_KEY_LEFT_SHIFT
        gdxGLFWKeyMap[Input.Keys.SHIFT_RIGHT] = GLFW.GLFW_KEY_RIGHT_SHIFT

        gdxGLFWKeyMap[Input.Keys.A] = GLFW.GLFW_KEY_A
        gdxGLFWKeyMap[Input.Keys.C] = GLFW.GLFW_KEY_C
        gdxGLFWKeyMap[Input.Keys.V] = GLFW.GLFW_KEY_V
        gdxGLFWKeyMap[Input.Keys.X] = GLFW.GLFW_KEY_X
        gdxGLFWKeyMap[Input.Keys.Y] = GLFW.GLFW_KEY_Y
        gdxGLFWKeyMap[Input.Keys.Z] = GLFW.GLFW_KEY_Z
    }

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
        gdxGLFWKeyMap[keycode]?.apply {
            LwjglGL3.keyCallback(this, 0, GLFW.GLFW_RELEASE, 0)
        }

        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        keyDownSignal(keycode)

        gdxGLFWKeyMap[keycode]?.apply {
            LwjglGL3.keyCallback(this, 0, GLFW.GLFW_PRESS, 0)
        }

        return false
    }
}