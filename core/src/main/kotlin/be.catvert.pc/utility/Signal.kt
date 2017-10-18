package be.catvert.pc.utility

class SignalListener<T>(val onSignal: (T) -> Unit)

class Signal<T> {
    private val listeners = mutableSetOf<SignalListener<T>>()

    fun register(listener: SignalListener<T>) {
        listeners.add(listener)
    }

    fun invokeSignal(value: T) {
        listeners.forEach { it.onSignal(value) }
    }
}