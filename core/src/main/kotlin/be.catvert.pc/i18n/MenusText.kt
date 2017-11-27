package be.catvert.pc.i18n

enum class MenusText(val key: String) {
    MM_WINDOW_TITLE("mm_windowTitle"),
    MM_PLAY_BUTTON("mm_playButton"),
    MM_SETTINGS_BUTTON("mm_settingsButton"),
    MM_EXIT_BUTTON("mm_exitButton"),

    MM_SELECT_LEVEL_WINDOW_TITLE("mm_selectLevel_windowTitle"),
    MM_SELECT_LEVEL_PLAY_BUTTON("mm_selectLevel_playButton"),
    MM_SELECT_LEVEL_EDIT_BUTTON("mm_selectLevel_editButton"),
    MM_SELECT_LEVEL_NEW_BUTTON("mm_selectLevel_newButton"),
    MM_SELECT_LEVEL_COPY_BUTTON("mm_selectLevel_copyButton"),
    MM_SELECT_LEVEL_DELETE_BUTTON("mm_selectLevel_deleteButton"),

    MM_SELECT_LEVEL_DELETE_DIALOG_TITLE("mm_selectLevelDeleteDialogTitle"),
    MM_SELECT_LEVEL_DELETE_DIALOG_CONTENT("mm_selectLevelDeleteDialogContent"),
    MM_SELECT_LEVEL_DELETE_DIALOG_YES("mm_selectLevelDeleteDialogYes"),
    MM_SELECT_LEVEL_DELETE_DIALOG_NO("mm_selectLevelDeleteDialogNo"),

    MM_SELECT_LEVEL_NAME_WINDOW_TITLE("mm_selectLevelName_windowTitle"),
    MM_SELECT_LEVEL_NAME_NAME("mm_selectLevelName_name"),
    MM_SELECT_LEVEL_NAME_CONFIRM("mm_selectLevelName_confirm"),

    MM_SETTINGS_WINDOW_TITLE("mm_settingsWindowTitle"),
    MM_SETTINGS_SCREEN_WIDTH("mm_settingsScreenWidth"),
    MM_SETTINGS_SCREEN_HEIGHT("mm_settingsScreenHeight"),
    MM_SETTINGS_FULLSCREEN("mm_settingsFullscreen"),
    MM_SETTINGS_SOUND("mm_settingsSound"),
    MM_SETTINGS_APPLY("mm_settingsApply"),

    MM_WRONG_LEVEL_VERSION_DIALOG_TITLE("mm_wrongLevelVersionDialogTitle"),
    MM_WRONG_LEVEL_VERSION_DIALOG_CONTENT("mm_wrongLevelVersionDialogContent");

    operator fun invoke(vararg args: Any) = Locales.get(this, *args)
}