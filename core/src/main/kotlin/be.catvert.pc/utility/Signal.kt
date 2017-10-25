package be.catvert.pc.utility

class SignalListener<in T>(val onSignal: (T) -> Unit)

class Signal<T> {
    private val listeners = mutableSetOf<SignalListener<T>>()

    fun register(listener: SignalListener<T>) {
       listeners.add(listener)
    }

    fun register(onSignal: (T) -> Unit) = register(SignalListener(onSignal))

    operator fun invoke(value: T) {
        listeners.forEach { it.onSignal(value) }
    }
}