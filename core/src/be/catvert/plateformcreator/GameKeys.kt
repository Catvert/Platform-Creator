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
    EDITOR_FLIPX_0(Input.Keys.LEFT, "Editor flip x"),
    EDITOR_FLIPX_1(Input.Keys.RIGHT, "Editor flip x[1]"),
    EDITOR_FLIPY_0(Input.Keys.UP, "Editor flip y"),
    EDITOR_FLIPY_1(Input.Keys.DOWN, "Editor flip y[1]"),
    EDITOR_UP_LAYER(Input.Keys.PLUS, "Editor up select layer"),
    EDITOR_DOWN_LAYER(Input.Keys.MINUS, "Editor down select layer"),


    DEBUG_MODE(Input.Keys.F12, "Debug Mode"),

    GAME_SWITCH_GRAVITY(Input.Keys.G, "Switch gravity on/off"),
    GAME_FOLLOW_CAMERA_PLAYER(Input.Keys.F, "Follow camera on player on/off"),
    GAME_CAMERA_LEFT(Input.Keys.LEFT, "Game camera left move"),
    GAME_CAMERA_RIGHT(Input.Keys.RIGHT, "Game camera right move"),
    GAME_CAMERA_UP(Input.Keys.UP, "Game camera up move"),
    GAME_CAMERA_DOWN(Input.Keys.DOWN, "Game camera down move")
}