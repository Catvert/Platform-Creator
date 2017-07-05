package be.catvert.plateformcreator.ecs.components

import be.catvert.plateformcreator.Copyable
import com.badlogic.ashley.core.Component

/**
 * Created by Catvert on 08/06/17.
 */

/**
 * Cette classe permet d'ajouter au component une variable pour savoir si celui-ci est actif ou non
 * @property active Permet d'activer ou de désactiver le component
 */
abstract class BaseComponent<T : Component>(var active: Boolean = true) : Component, Copyable<T>