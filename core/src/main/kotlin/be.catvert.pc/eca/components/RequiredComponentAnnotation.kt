package be.catvert.pc.eca.components

import kotlin.reflect.KClass

/**
 * Ajoute la possibilité à un component ou une action de spécifier si un component est obligatoire pour son utilisation.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiredComponent(vararg val component: KClass<out Component>)