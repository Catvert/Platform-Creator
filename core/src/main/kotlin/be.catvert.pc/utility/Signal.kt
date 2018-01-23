package be.catvert.pc.utility

class SignalListener<in T>(val removeAfterInvoke: Boolean = false, val onSignal: (T) -> Unit)

class Signal<T> {
    private val listeners = mutableSetOf<SignalListener<T>>()

    fun register(listener: SignalListener<T>) {
        listeners.add(listener)
    }

    fun register(removeAfterInvoke: Boolean = false, onSignal: (T) -> Unit) = register(SignalListener(removeAfterInvoke, onSignal))

    fun removeListener(listener: SignalListener<T>) {
        listeners.remove(listener)
    }

    fun clear() {
        listeners.clear()
    }

    operator fun invoke(value: T) {
        listeners.forEach { it.onSignal(value) }
        listeners.removeAll { it.removeAfterInvoke }
    }
}