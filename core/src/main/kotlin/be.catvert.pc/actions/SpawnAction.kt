package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.Prefab
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.utility.BoxSide
import be.catvert.pc.utility.ExposeEditor
import be.catvert.pc.utility.Point
import com.fasterxml.jackson.annotation.JsonCreator

class SpawnAction(@ExposeEditor var prefab: Prefab, @ExposeEditor var spawnSide: BoxSide) : Action() {
    @JsonCreator private constructor() : this(PrefabFactory.MushroomRed_SMC.prefab, BoxSide.Left)

    override fun invoke(gameObject: GameObject) {
        var x = gameObject.box.x
        var y = gameObject.box.y

        when (spawnSide) {
            BoxSide.Left -> x -= prefab.prefabGO.box.width
            BoxSide.Right -> x += gameObject.box.width
            BoxSide.Up -> y += gameObject.box.height
            BoxSide.Down -> y -= prefab.prefabGO.box.height
        }

        gameObject.container?.apply {
            prefab.create(Point(x, y), this)
        }
    }

    override fun toString() = super.toString() + " - { prefab : $prefab ; side : $spawnSide }"
}