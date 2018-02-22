package be.catvert.pc.utility

/**
 * Chronomètre permettant d'effectuer une action à chacune de ses intervals
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