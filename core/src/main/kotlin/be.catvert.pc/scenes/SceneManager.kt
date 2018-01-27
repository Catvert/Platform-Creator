package be.catvert.pc.scenes

import be.catvert.pc.Log
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Disposable
import imgui.ImGui
import imgui.impl.LwjglGL3
import ktx.app.clearScreen

class SceneManager(initialScene: Scene) : Updeatable, Renderable, Resizable, Disposable {
    private var currentScene: Scene = initialScene
    private var nextScene: NextWaitingScene? = null

    fun currentScene() = currentScene

    private var isTransitionRunning = false

    private data class NextWaitingScene(val scene: Scene, val applyTransition: Boolean, val disposeCurrentScene: Boolean)

    private var waitingScene = mutableMapOf<Class<Scene>, NextWaitingScene>()

    private val interpolation = Interpolation.linear

    private var elapsedTime = 0f

    fun loadScene(scene: Scene, applyTransition: Boolean = true, disposeCurrentScene: Boolean = true) {
        if (applyTransition) {
            if (isTransitionRunning) {
                waitingScene[scene.javaClass] = NextWaitingScene(scene, applyTransition, disposeCurrentScene)
                return
            }

            scene.alpha = 0f
            currentScene.alpha = 1f

            elapsedTime = 0f

            nextScene = NextWaitingScene(scene, applyTransition, disposeCurrentScene)

            isTransitionRunning = true
        } else {
            if (isTransitionRunning)
                waitingScene[scene.javaClass] = NextWaitingScene(scene, applyTransition, disposeCurrentScene)
            else
                setScene(scene, false)
        }
    }

    private fun setScene(scene: Scene, disposeCurrentScene: Boolean) {
        if (disposeCurrentScene)
            currentScene.dispose()

        Log.info { "Chargement de la scène : ${ReflectionUtility.simpleNameOf(scene)}" }
        currentScene = scene
    }

    override fun update() {
        if (nextScene != null && isTransitionRunning) {
            val (nextScene, _, disposeCurrentScene) = nextScene!!

            elapsedTime += Gdx.graphics.deltaTime
            val progress = Math.min(1f, elapsedTime / 1f)

            currentScene.alpha = interpolation.apply(1f, 0f, progress)
            nextScene.alpha = interpolation.apply(0f, 1f, progress)

            nextScene.update()

            if (progress == 1f) {
                setScene(nextScene, disposeCurrentScene)

                this@SceneManager.nextScene = null
                isTransitionRunning = false

                if (waitingScene.isNotEmpty()) {
                    val scene = waitingScene.entries.elementAt(0)
                    waitingScene.remove(scene.key)
                    val (nextScene, applyTransition, disposeCurrentScene) = scene.value
                    loadScene(nextScene, applyTransition, disposeCurrentScene)
                }
            }
        } else
            currentScene.update()
    }

    override fun render(batch: Batch) {
        clearScreen(currentScene.backgroundColors.first, currentScene.backgroundColors.second, currentScene.backgroundColors.third)

        LwjglGL3.newFrame()

        ImGui.style.alpha = currentScene.alpha
        batch.setColor(1f, 1f, 1f, currentScene.alpha)
        currentScene.render(batch)

        ImGui.render()

        nextScene?.apply {
            ImGui.newFrame()

            ImGui.style.alpha = scene.alpha
            batch.setColor(1f, 1f, 1f, scene.alpha)
            scene.render(batch)

            ImGui.render()
        }
    }

    override fun resize(size: Size) {
        currentScene.resize(size)
    }

    override fun dispose() {
        currentScene.dispose()
    }
}