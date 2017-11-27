package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.containers.Level
import be.catvert.pc.utility.ExposeEditor
import com.fasterxml.jackson.annotation.JsonCreator

class ScoreAction(@ExposeEditor(maxInt = 100) var points: Int) : Action {
    @JsonCreator private constructor() : this(0)

    override fun invoke(gameObject: GameObject) {
        (gameObject.container as? Level)?.also {
            it.scorePoints += points
        }
    }
}