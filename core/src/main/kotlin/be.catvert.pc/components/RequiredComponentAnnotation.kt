package be.catvert.pc.components

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiredComponent(vararg val component: KClass<out Component>)