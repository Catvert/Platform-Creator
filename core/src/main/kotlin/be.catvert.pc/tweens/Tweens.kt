package be.catvert.pc.tweens

import be.catvert.pc.GameObject
import be.catvert.pc.actions.Action
import be.catvert.pc.components.graphics.AtlasComponent
import be.catvert.pc.utility.Point
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Interpolation
import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.math.roundToInt

abstract class Tween(var duration: Float = 1f, @JsonIgnore val interpolation: Interpolation) {
    var nextTween: Tween? = null
    var endAction: Action? = null

    var useTweenState: Boolean = false

    private var elapsedTime: Float = 0f

    protected var progress: Float = 0f
        private set

    open fun init(gameObject: GameObject) {
        elapsedTime = 0f
    }

    fun update(gameObject: GameObject): Boolean {
        elapsedTime += Gdx.graphics.deltaTime
        progress = Math.min(1f, elapsedTime / duration)

        perform(gameObject)
        return elapsedTime >= duration
    }

    abstract fun perform(gameObject: GameObject)
}

class EmptyTween(): Tween(0f, Interpolation.linear) {
    override fun perform(gameObject: GameObject) {}
}

class MoveTween(duration: Float, var moveX: Int, var moveY: Int) : Tween(duration, Interpolation.linear) {
    private var initialPosX = 0f
    private var initialPosY = 0f

    override fun init(gameObject: GameObject) {
        super.init(gameObject)

        initialPosX = gameObject.position().x.toFloat()
        initialPosY = gameObject.position().y.toFloat()
    }

    override fun perform(gameObject: GameObject) {
        gameObject.box.position = Point(interpolation.apply(initialPosX, initialPosX + moveX, progress).roundToInt(),
                interpolation.apply(initialPosY, initialPosY + moveY, progress).roundToInt())
    }
}

class AlphaAtlasTween(duration: Float, var targetAlpha: Float = 0f) : Tween(duration, Interpolation.linear) {
    private var initialAlpha = 0f

    override fun init(gameObject: GameObject) {
        super.init(gameObject)

        initialAlpha = gameObject.getCurrentState().getComponent<AtlasComponent>()?.alpha ?: 0f
    }

    override fun perform(gameObject: GameObject) {
        gameObject.getCurrentState().getComponent<AtlasComponent>()?.apply {
            alpha = interpolation.apply(initialAlpha, targetAlpha, progress)
        }
    }
}