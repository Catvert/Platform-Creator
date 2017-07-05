package be.catvert.plateformcreator

/**
 * Created by catvert on 5/07/17.
 */

/**
 * Interface permettant d'implémenter la deep-copy
 */
interface Copyable<T> {
    fun copy(): T
}