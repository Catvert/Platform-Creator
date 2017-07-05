package be.catvert.plateformcreator

import com.badlogic.gdx.Input

/**
 * Created by Catvert on 18/06/17.
 */

/**
 * Enumération des différentes touches utilisées dans le jeu
 */
enum class GameKeys(var key: Int, val description: String) {
    EDITOR_CAMERA_LEFT(Input.Keys.Q, "Editor camera left move"),
    EDITOR_CAMERA_RIGHT(Input.Keys.D, "Editor camera right move"),
    EDITOR_CAMERA_UP(Input.Keys.Z, "Editor camera up move"),
    EDITOR_CAMERA_DOWN(Input.Keys.S, "Editor camera down move"),
    CAMERA_ZOOM_UP(Input.Keys.P, "Camera zoom up"),
    CAMERA_ZOOM_DOWN(Input.Keys.M, "Camera zoom down"),
    CAMERA_ZOOM_RESET(Input.Keys.L, "Camera zoom reset"),

    EDITOR_REMOVE_ENTITY(Input.Keys.DEL, "Remove entity from editor"),
    EDITOR_COPY_MODE(Input.Keys.C, "Copy entity mode in editor"),
    EDITOR_APPEND_SELECT_ENTITIES(Input.Keys.CONTROL_LEFT, "Append select entities in editor"),
    EDITOR_SMARTCOPY_LEFT(Input.Keys.LEFT, "Editor smart copy left"),
    EDITOR_SMARTCOPY_RIGHT(Input.Keys.RIGHT, "Editor smart copy right"),
    EDITOR_SMARTCOPY_UP(Input.Keys.UP, "Editor smart copy up"),
    EDITOR_SMARTCOPY_DOWN(Input.Keys.DOWN, "Editor smart copy down"),
    EDITOR_FLIPX(Input.Keys.X, "Editor flip x"),
    EDITOR_FLIPY(Input.Keys.Y, "Editor flip y"),
    EDITOR_UP_LAYER(Input.Keys.PLUS, "Editor up select layer"),
    EDITOR_DOWN_LAYER(Input.Keys.MINUS, "Editor down select layer"),
    EDITOR_SWITCH_RESIZE_MODE(Input.Keys.R, "Editor switch resize mode"),
    EDITOR_MOVE_ENTITY_LEFT(Input.Keys.LEFT, "Editor move entity 1 pixel left"),
    EDITOR_MOVE_ENTITY_RIGHT(Input.Keys.RIGHT, "Editor move entity 1 pixel right"),
    EDITOR_MOVE_ENTITY_UP(Input.Keys.UP, "Editor move entity 1 pixel up"),
    EDITOR_MOVE_ENTITY_DOWN(Input.Keys.DOWN, "Editor move entity 1 pixel down"),


    DEBUG_MODE(Input.Keys.F12, "Debug Mode"),

    GAME_SWITCH_GRAVITY(Input.Keys.G, "Switch gravity on/off"),
    GAME_FOLLOW_CAMERA_PLAYER(Input.Keys.F, "Follow camera on player on/off"),
    GAME_CAMERA_LEFT(Input.Keys.LEFT, "Game camera left move"),
    GAME_CAMERA_RIGHT(Input.Keys.RIGHT, "Game camera right move"),
    GAME_CAMERA_UP(Input.Keys.UP, "Game camera up move"),
    GAME_CAMERA_DOWN(Input.Keys.DOWN, "Game camera down move")
}