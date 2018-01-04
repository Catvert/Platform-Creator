package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant d'ajouter des points de score au joueur
 */
class ScoreAction(@ExposeEditor(max = 100) var points: Int) : Action {
    @JsonCreator private constructor() : this(0)

    override fun invoke(gameObject: GameObject) {
        gameObject.container.cast<Level>()?.apply {
                    scorePoints += points
                }
    }
}