package be.catvert.pc.utility

import com.badlogic.gdx.Gdx

class Timer(var interval: Float) : Updeatable {
    private var timerDelta = 0f

    var timer = 0

    val onIncrement = Signal<Int>()

    override fun update() {
        timerDelta += Gdx.graphics.deltaTime
        if (timerDelta >= interval) {
            ++timer
            timerDelta = 0f

            onIncrement(timer)
        }
    }
}