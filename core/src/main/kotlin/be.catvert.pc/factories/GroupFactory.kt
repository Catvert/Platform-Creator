package be.catvert.pc.factories

import be.catvert.pc.builders.EntityBuilder
import be.catvert.pc.builders.TweenBuilder
import be.catvert.pc.eca.*
import be.catvert.pc.eca.actions.InputAction
import be.catvert.pc.eca.actions.TeleportSideAction
import be.catvert.pc.eca.actions.TweenAction
import be.catvert.pc.eca.components.graphics.PackRegionData
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.components.graphics.TextureGroup
import be.catvert.pc.eca.components.logics.CollisionAction
import be.catvert.pc.eca.components.logics.InputComponent
import be.catvert.pc.eca.components.logics.PhysicsComponent
import be.catvert.pc.tweens.MoveTween
import be.catvert.pc.utility.*
import com.badlogic.gdx.Input

enum class GroupFactory(val group: () -> Group) {
    Pipe({
        val pipeEndID = EntityID(3)
        Group("pipes", arrayListOf(pipeEndID),
                Prefab("pipe start",
                        EntityBuilder(Tags.Special.tag, Size(100, 50)).withDefaultState {
                            withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("pipes.atlas").toFileWrapper()), "blue/up"))))
                            withComponent(PhysicsComponent(true,
                                    collisionsActions = arrayListOf(CollisionAction(BoxSide.Up, action = InputAction(InputComponent.InputData(Input.Keys.S, false,
                                            TweenAction(TweenBuilder(MoveTween(0.5f, 0, -50)).build(TeleportSideAction(pipeEndID, BoxSide.Up, true))))), applyActionOnCollider = true))))
                        }.withLayer(1).build()
                ),
                EntityDescriptor(
                        Prefab("pipe start body",
                                EntityBuilder(Tags.Special.tag, Size(100, 50)).withDefaultState {
                                    withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("pipes.atlas").toFileWrapper()), "blue/ver"))))
                                    withComponent(PhysicsComponent(true))
                                }.withLayer(1).build()), Point(0f, -50f)),
                EntityDescriptor(
                        Prefab("pipe end",
                                EntityBuilder(Tags.Special.tag, Size(100, 50)).withDefaultState {
                                    withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("pipes.atlas").toFileWrapper()), "green/up"))))
                                    withComponent(PhysicsComponent(true))
                                }.withLayer(1).build()), Point(200f, 0f)),
                EntityDescriptor(
                        Prefab("pipe end body",
                                EntityBuilder(Tags.Special.tag, Size(100, 50)).withDefaultState {
                                    withComponent(TextureComponent(0, TextureGroup("default", PackRegionData(resourceWrapperOf(Constants.packsSMCDirPath.child("pipes.atlas").toFileWrapper()), "green/ver"))))
                                    withComponent(PhysicsComponent(true))
                                }.withLayer(1).build()), Point(200f, -50f))
        )
    });
}