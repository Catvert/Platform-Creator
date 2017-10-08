package be.catvert.pc.actions

import be.catvert.pc.GameObject
import be.catvert.pc.serialization.InheritanceAdapter
import com.google.gson.annotations.JsonAdapter

@JsonAdapter(ActionResolverAdapter::class)
interface Action {
    fun perform(gameObject: GameObject)
}

class ActionResolverAdapter : InheritanceAdapter<Action>()