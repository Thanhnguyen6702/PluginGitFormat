package com.thanhnguyen.git.format.settings

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.OptionTag

class GitSettingsState : BaseState() {

    @OptionTag("SETTINGS_VERSION")
    var settingsVersion: Int = CURRENT_VERSION

    @OptionTag("DEFAULT_BASE_BRANCH")
    var defaultBaseBranch: String = "develop"

    companion object {
        const val CURRENT_VERSION = 3
    }

    fun resetToDefaults() {
        defaultBaseBranch = "develop"
        settingsVersion = CURRENT_VERSION
    }
}
