package be.catvert.pc.scenes

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Disposable
import imgui.ImGui
import imgui.impl.LwjglGL3
import ktx.app.clearScreen

object SceneManager : Updeatable, Renderable, Resizable, Disposable {
    private var currentScene: Scene = MainMenuScene()
    private var nextScene: Scene? = null

    fun currentScene() = currentScene

    private var isTransitionRunning = false

    private data class NextWaitingScene(val scene: Scene, val applyTransition: Boolean, val disposeCurrentScene: Boolean)

    private var waitingScene = mutableMapOf<Class<Scene>, NextWaitingScene>()

    fun loadScene(scene: Scene, applyTransition: Boolean = true, disposeCurrentScene: Boolean = true) {
        if (applyTransition) {
            if (isTransitionRunning) {
                waitingScene[scene.javaClass] = NextWaitingScene(scene, applyTransition, disposeCurrentScene)
                return
            }
            nextScene = scene

            nextScene?.alpha = 0f
            currentScene.alpha = 1f

            isTransitionRunning = true

            Timeline.createSequence()
                    .beginParallel()
                    .push(Tween.to(currentScene, SceneTweenAccessor.SceneTween.ALPHA.tweenType, 0.5f).target(0f).setCallback { _, _ ->
                        setScene(scene, disposeCurrentScene)
                    })
                    .push(Tween.to(scene, SceneTweenAccessor.SceneTween.ALPHA.tweenType, 0.5f).target(1f).setCallback { _, _ ->
                        nextScene = null
                        isTransitionRunning = false

                        if (waitingScene.isNotEmpty()) {
                            val scene = waitingScene.entries.elementAt(0)
                            waitingScene.remove(scene.key)
                            val (nextScene, applyTransition, disposeCurrentScene) = scene.value
                            loadScene(nextScene, applyTransition, disposeCurrentScene)
                        }
                    })
                    .end()
                    .start(PCGame.tweenManager)
        } else {
            if (isTransitionRunning)
                waitingScene[scene.javaClass] = NextWaitingScene(scene, applyTransition, disposeCurrentScene)
            else
                setScene(scene, disposeCurrentScene)
        }
    }

    private fun setScene(scene: Scene, disposeCurrentScene: Boolean) {
        if (disposeCurrentScene)
            currentScene.dispose()

        Log.info { "Chargement de la scène : ${ReflectionUtility.simpleNameOf(scene)}" }
        currentScene = scene
    }

    override fun update() {
        nextScene?.update()
        if (nextScene == null)
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

            ImGui.style.alpha = alpha
            batch.setColor(1f, 1f, 1f, alpha)
            render(batch)

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