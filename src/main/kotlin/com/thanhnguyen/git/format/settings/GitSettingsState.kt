package com.thanhnguyen.git.format.settings

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.OptionTag

class GitSettingsState : BaseState() {

    // ===== VERSIONING =====
    @OptionTag("SETTINGS_VERSION")
    var settingsVersion: Int = CURRENT_VERSION

    // ===== MAIN SETTINGS =====
    @OptionTag("GITHUB_TOKEN")
    var githubToken: String = ""

    @OptionTag("JIRA_URL")
    var jiraUrl: String = ""

    @OptionTag("JIRA_EMAIL")
    var jiraEmail: String = ""

    @OptionTag("JIRA_API_TOKEN")
    var jiraApiToken: String = ""

    @OptionTag("DEFAULT_BASE_BRANCH")
    var defaultBaseBranch: String = "develop"

    companion object {
        const val CURRENT_VERSION = 3
    }

    /**
     * Reset all settings to their default values
     */
    fun resetToDefaults() {
        githubToken = ""
        jiraUrl = ""
        jiraEmail = ""
        jiraApiToken = ""
        defaultBaseBranch = "develop"
        settingsVersion = CURRENT_VERSION
    }
}
