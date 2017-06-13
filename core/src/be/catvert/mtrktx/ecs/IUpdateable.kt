package be.catvert.mtrktx.ecs

/**
* Created by Catvert on 04/06/17.
*/

interface IUpdateable {
    fun update(deltaTime: Float)
    operator fun  invoke(deltaTime: Float) {
        update(deltaTime)
    }
}