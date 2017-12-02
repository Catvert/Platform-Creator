package be.catvert.pc.scenes

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import be.catvert.pc.Log
import be.catvert.pc.PCGame
import be.catvert.pc.utility.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Disposable
import imgui.ImGui
import ktx.app.clearScreen

object SceneManager : Updeatable, Renderable, Resizable, Disposable {
    private var currentScene: Scene = MainMenuScene()
    private var nextScene: Scene? = null

    fun currentScene() = currentScene

    private var isTransitionRunning = false

    private data class NextWaitingScene(val scene: Scene, val applyTransition: Boolean, val disposeCurrentScene: Boolean)

    private var waitingScene = mutableListOf<NextWaitingScene>()

    fun loadScene(scene: Scene, applyTransition: Boolean = true, disposeCurrentScene: Boolean = true) {
        if(applyTransition) {
            if(isTransitionRunning) {
                waitingScene.add(NextWaitingScene(scene, applyTransition, disposeCurrentScene))
                return
            }
            currentScene.alpha = 1f
            scene.alpha = 0f
            isTransitionRunning = true

            nextScene = scene

            Timeline.createSequence()
                    .beginParallel()
                    .push(Tween.to(currentScene, SceneTweenAccessor.SceneTween.ALPHA.tweenType, 0.5f).target(0f).setCallback { _, _ ->
                        setScene(scene, disposeCurrentScene)
                    })
                    .push(Tween.to(scene, SceneTweenAccessor.SceneTween.ALPHA.tweenType, 0.5f).target(1f).setCallback { _, _ ->
                        nextScene = null
                        isTransitionRunning = false

                        if(waitingScene.isNotEmpty()) {
                            val scene = waitingScene[0]
                            waitingScene.remove(scene)
                            loadScene(scene.scene, scene.applyTransition, scene.disposeCurrentScene)
                        }
                    })
                    .end()
                    .start(PCGame.tweenManager)
        }
        else {
            if(isTransitionRunning)
                waitingScene.add(NextWaitingScene(scene, applyTransition, disposeCurrentScene))
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

        batch.setColor(1f, 1f, 1f, currentScene.alpha)
        ImGui.style.alpha = currentScene.alpha
        currentScene.render(batch)

        nextScene?.apply {
            batch.setColor(1f, 1f, 1f, alpha)
            ImGui.style.alpha = alpha
            render(batch)
        }
    }

    override fun resize(size: Size) {
        currentScene.resize(size)
    }

    override fun dispose() {
        currentScene.dispose()
    }
}