package com.thanhnguyen.git.format.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*

@State(
    name = GitSettings.NAME, 
    storages = [Storage(GitSettings.STORAGES, roamingType = RoamingType.DISABLED)], 
    category = SettingsCategory.TOOLS
)
class GitSettings : PersistentStateComponent<GitSettingsState> {

    private val state = GitSettingsState()
    
    // PasswordSafe credentials for secure storage
    private val githubTokenCredentials = CredentialAttributes(
        generateServiceName("GitCommitFormat", "GitHub Token")
    )
    
    private val jiraTokenCredentials = CredentialAttributes(
        generateServiceName("GitCommitFormat", "JIRA Token")
    )

    private val jiraUrlCredentials = CredentialAttributes(
        generateServiceName("GitCommitFormat", "JIRA URL")
    )
    
    private val jiraEmailCredentials = CredentialAttributes(
        generateServiceName("GitCommitFormat", "JIRA Email")
    )

    override fun getState(): GitSettingsState = state

    override fun loadState(state: GitSettingsState) {
        // Only keep default branch in XML state
        this.state.defaultBaseBranch = state.defaultBaseBranch
    }

    var githubToken: String
        get() = PasswordSafe.instance.getPassword(githubTokenCredentials) ?: ""
        set(value) {
            if (value.isBlank()) {
                PasswordSafe.instance.setPassword(githubTokenCredentials, null)
            } else {
                PasswordSafe.instance.setPassword(githubTokenCredentials, value)
            }
        }

    var jiraUrl: String
        get() {
            val saved = PasswordSafe.instance.getPassword(jiraUrlCredentials)
            return if (saved.isNullOrBlank()) "https://apero.atlassian.net/" else saved
        }
        set(value) {
            val newValue = if (value.isBlank()) "https://apero.atlassian.net/" else value
            PasswordSafe.instance.setPassword(jiraUrlCredentials, newValue)
        }

    var jiraEmail: String
        get() = PasswordSafe.instance.getPassword(jiraEmailCredentials) ?: ""
        set(value) {
            if (value.isBlank()) {
                PasswordSafe.instance.setPassword(jiraEmailCredentials, null)
            } else {
                PasswordSafe.instance.setPassword(jiraEmailCredentials, value)
            }
        }

    var jiraApiToken: String
        get() = PasswordSafe.instance.getPassword(jiraTokenCredentials) ?: ""
        set(value) {
            if (value.isBlank()) {
                PasswordSafe.instance.setPassword(jiraTokenCredentials, null)
            } else {
                PasswordSafe.instance.setPassword(jiraTokenCredentials, value)
            }
        }

    var defaultBaseBranch: String
        get() = state.defaultBaseBranch
        set(value) {
            state.defaultBaseBranch = value
        }

    fun isGitHubConfigured(): Boolean {
        return githubToken.isNotBlank()
    }

    fun isJiraConfigured(): Boolean {
        return jiraUrl.isNotBlank() && jiraEmail.isNotBlank() && jiraApiToken.isNotBlank()
    }

    fun resetToDefaults() {
        state.resetToDefaults()
        
        // Clear all data from PasswordSafe
        PasswordSafe.instance.setPassword(githubTokenCredentials, null)
        PasswordSafe.instance.setPassword(jiraTokenCredentials, null)
        PasswordSafe.instance.setPassword(jiraUrlCredentials, null)
        PasswordSafe.instance.setPassword(jiraEmailCredentials, null)
    }

    companion object {
        const val NAME = "GitCommitMessageFormat"
        const val STORAGES = "git-commit-message-format.xml"

        @JvmStatic
        val instance: GitSettings
            get() = service()
    }
}
