package be.catvert.mtrktx.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

/**
* Created by Catvert on 08/06/17.
*/

abstract class BaseComponent(var active: Boolean = true): Component {
    abstract fun copy(target: Entity): BaseComponent
}