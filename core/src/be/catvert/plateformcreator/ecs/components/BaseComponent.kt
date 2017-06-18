package be.catvert.plateformcreator.ecs.components

import com.badlogic.ashley.core.Component

/**
 * Created by Catvert on 08/06/17.
 */

/**
 * Cette classe permet d'ajouter au component une variable pour savoir si il est actif ou non et une méthode de copie
 */
abstract class BaseComponent<out T : Component>(var active: Boolean = true) : Component {
    /**
     * Permet de copier le component en deep copy
     */
    abstract fun copy(): T
}