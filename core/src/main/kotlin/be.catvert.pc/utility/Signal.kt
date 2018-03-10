package be.catvert.pc.utility

class SignalListener<in T>(var cancel: Boolean = false, val removeAfterInvoke: Boolean = false, val onSignal: (T) -> Unit)

/**
 * Représente un événement
 */
class Signal<T> {
    private val listeners = mutableSetOf<SignalListener<T>>()

    fun register(listener: SignalListener<T>): SignalListener<T> {
        listeners.add(listener)
        return listener
    }

    fun register(removeAfterInvoke: Boolean = false, onSignal: (T) -> Unit) = register(SignalListener(false, removeAfterInvoke, onSignal))

    fun clear() {
        listeners.clear()
    }

    operator fun invoke(value: T) {
        listeners.filter { !it.cancel }.forEach { it.onSignal(value) }
        listeners.removeAll { it.removeAfterInvoke || it.cancel }
    }
}