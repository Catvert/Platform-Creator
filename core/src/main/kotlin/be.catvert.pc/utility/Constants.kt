package be.catvert.pc.utility

import be.catvert.pc.serialization.SerializationFactory
import ktx.assets.toLocalFile

/**
 * Objet permettant d'accéder aux antes du jeu
 */
object Constants {
    const val gameTitle = "Platform-Creator"
    const val gameVersion = 2.0f

    const val maxGameObjectSize = 500
    const val minLayerIndex = -100
    const val maxLayerIndex = 100

    const val minMatrixSize = 10
    const val matrixCellSize = 200

    const val viewportRatioWidth = 1920f
    const val viewportRatioHeight = 1080f

    const val defaultWidgetsWidth = 125f

    const val assetsDir = "assets/"

    val serializationType = SerializationFactory.MapperType.JSON

    private val assetsDirPath = "assets/".toLocalFile()

    const val configPath = assetsDir + "config.json"

    val bundlesDirPath = assetsDirPath.child("i18n_bundles")

    val uiDirPath = assetsDirPath.child("ui/")

    val fontDirPath = assetsDirPath.child("fonts/")
    val imguiFontPath = fontDirPath.child("ui.ttf")
    val kenneyFontPath = fontDirPath.child("kenvector_future.ttf")
    val kenneyThinFontPath = fontDirPath.child("kenvector_future_thin.ttf")
    val mainFontPath = fontDirPath.child("mainFont.fnt")
    val editorFontPath = fontDirPath.child("editorFont.fnt")

    val keysConfigPath = assetsDirPath.child("keysConfig.json")

    val packsDirPath = assetsDirPath.child("packs")
    val packsKenneyDirPath = packsDirPath.child("kenney")
    val packsSMCDirPath = packsDirPath.child("smc")

    val texturesDirPath = assetsDirPath.child("textures")
    val soundsDirPath = assetsDirPath.child("sounds")
    val backgroundsDirPath = assetsDirPath.child("backgrounds")
    val levelDirPath = assetsDirPath.child("levels")

    val gameBackgroundMenuPath = assetsDirPath.child("game/mainmenu.png")
    val gameLogoPath = assetsDirPath.child("game/logo.png")

    val prefabExtension = "prefab"
    val levelExtension = "pclvl"

    val levelTextureExtension = arrayOf("jpg", "png")
    val levelAtlasExtension = arrayOf("atlas")
    val levelSoundExtension = arrayOf("mp3", "wav", "ogg")

    val levelDataFile = "data.$levelExtension"

    val defaultSoundPath = assetsDirPath.child("game/nosound.wav")
}