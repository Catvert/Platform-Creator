package be.catvert.pc.i18n

import be.catvert.pc.Log
import be.catvert.pc.utility.Constants
import com.badlogic.gdx.utils.I18NBundle
import java.util.*

object Locales {
    val menusPath = Constants.bundlesDirPath.child("menus/bundle")
    val gamePath = Constants.bundlesDirPath.child("game/bundle")
    val editorPath = Constants.bundlesDirPath.child("editor/bundle")

    private lateinit var menusBundle: I18NBundle
    private lateinit var gameBundle: I18NBundle
    private lateinit var editorBundle: I18NBundle

    fun load(locale: Locale) {
        menusBundle = I18NBundle.createBundle(menusPath, locale)
        gameBundle = I18NBundle.createBundle(gamePath, locale)
        editorBundle = I18NBundle.createBundle(editorPath, locale)
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