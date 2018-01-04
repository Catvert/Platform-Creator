package be.catvert.pc.i18n

import be.catvert.pc.Log
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.ResourceManager
import com.badlogic.gdx.utils.I18NBundle
import java.io.FileNotFoundException
import java.util.*

object Locales {
    private val menusPath = Constants.bundlesDirPath.child("menus/bundle")
    private val gamePath = Constants.bundlesDirPath.child("game/bundle")
    private val editorPath = Constants.bundlesDirPath.child("editor/bundle")

    private lateinit var menusBundle: I18NBundle
    private lateinit var gameBundle: I18NBundle
    private lateinit var editorBundle: I18NBundle

    fun load() {
        menusBundle = ResourceManager.getI18NBundle(menusPath) ?: throw FileNotFoundException("Impossible de trouver le fichier de traduction -> ${menusPath.path()}")
        gameBundle = ResourceManager.getI18NBundle(gamePath) ?: throw FileNotFoundException("Impossible de trouver le fichier de traduction -> ${gamePath.path()}")
        editorBundle = ResourceManager.getI18NBundle(editorPath) ?: throw FileNotFoundException("Impossible de trouver le fichier de traduction -> ${editorPath.path()}")
    }

    fun get(key: MenusText, vararg args: Any): String {
        try {
            return menusBundle.format(key.key, *args)
        } catch (e: MissingResourceException) {
            Log.error(e) { "Impossible de charger la traduction de ${key.key}" }
        }
        return key.key
    }

    fun get(key: GameText, vararg args: Any): String {
        try {
            return gameBundle.format(key.key, *args)
        } catch (e: MissingResourceException) {
            Log.error(e) { "Impossible de charger la traduction de ${key.key}" }
        }
        return key.key
    }

    fun get(key: EditorText, vararg args: Any): String {
        try {
            return editorBundle.format(key.key, *args)
        } catch (e: MissingResourceException) {
            Log.error(e) { "Impossible de charger la traduction de ${key.key}" }
        }
        return key.key
    }
}