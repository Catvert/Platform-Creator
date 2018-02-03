package be.catvert.pc.utility

/**
 * Permet d'effectuer une action à chaque interval du chronomètre.
 */
class Timer(var interval: Float) : Updeatable {
    private var timerDelta = 0f

    var timer = 0

    val onIncrement = Signal<Int>()

    override fun update() {
        timerDelta += Utility.getDeltaTime()
        if (timerDelta >= interval) {
            ++timer
            timerDelta = 0f

            onIncrement(timer)
        }
    }
}