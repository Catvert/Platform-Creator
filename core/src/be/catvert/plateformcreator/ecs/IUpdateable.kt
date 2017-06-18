package be.catvert.plateformcreator.ecs

/**
* Created by Catvert on 04/06/17.
*/

/**
 * Interface permettant d'implémenter une méthode de mise à jour
 */
interface IUpdateable {
    fun update(deltaTime: Float)

    operator fun invoke(deltaTime: Float) {
        update(deltaTime)
    }
}