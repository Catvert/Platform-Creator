package be.catvert.pc.managers

import be.catvert.pc.Log
import be.catvert.pc.scenes.Scene
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Disposable
import imgui.ImGui
import imgui.impl.LwjglGL3
import ktx.app.clearScreen

/**
 * Permet de gérer les scènes
 */
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

            elapsedTime += Utility.getDeltaTime()
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
        // Permet d'effacer l'écran avec la couleur de la prochaine scène(si on est en transition) pour améliorer la cohérence de la transition.
        if (nextScene != null)
            clearScreen(nextScene!!.scene.backgroundColors[0], nextScene!!.scene.backgroundColors[1], nextScene!!.scene.backgroundColors[2])
        else
            clearScreen(currentScene.backgroundColors[0], currentScene.backgroundColors[1], currentScene.backgroundColors[2])

        LwjglGL3.newFrame()

        ImGui.style.alpha = currentScene.alpha
        batch.setColor(1f, 1f, 1f, currentScene.alpha)
        currentScene.viewport.apply()
        currentScene.render(batch)

        ImGui.render()

        if (ImGui.drawData != null)
            LwjglGL3.renderDrawData(ImGui.drawData!!)

        nextScene?.apply {
            ImGui.newFrame()

            ImGui.style.alpha = scene.alpha
            batch.setColor(1f, 1f, 1f, scene.alpha)
            scene.viewport.apply()
            scene.render(batch)

            ImGui.render()

            if (ImGui.drawData != null)
                LwjglGL3.renderDrawData(ImGui.drawData!!)
        }
    }

    override fun resize(size: Size) {
        currentScene.resize(size)
    }

    override fun dispose() {
        currentScene.dispose()
    }
}