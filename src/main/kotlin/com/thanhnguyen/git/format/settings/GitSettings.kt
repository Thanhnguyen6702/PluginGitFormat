package com.thanhnguyen.git.format.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = GitSettings.NAME, 
    storages = [Storage(GitSettings.STORAGES, roamingType = RoamingType.DISABLED)], 
    category = SettingsCategory.TOOLS
)
class GitSettings : PersistentStateComponent<GitSettingsState> {

    private val logger = Logger.getInstance(GitSettings::class.java)
    private val state = GitSettingsState()
    
    // PasswordSafe credentials for secure token storage
    private val githubTokenCredentials = CredentialAttributes(
        generateServiceName("GitCommitFormat", "GitHub Token")
    )
    
    private val jiraTokenCredentials = CredentialAttributes(
        generateServiceName("GitCommitFormat", "JIRA Token")
    )

    override fun getState(): GitSettingsState? {
        logger.info("💾 Saving settings to storage: $STORAGES")
        return state
    }

    override fun loadState(state: GitSettingsState) {
        try {
            XmlSerializerUtil.copyBean(state, this.state)
            
            logger.info("✅ Settings loaded successfully")
            logger.info("   Storage: $STORAGES")
            logger.info("   Version: ${this.state.settingsVersion}")
            logger.info("   GitHub configured: ${isGitHubConfigured()}")
            logger.info("   JIRA configured: ${isJiraConfigured()}")
            logger.info("   Default branch: ${this.state.defaultBaseBranch}")
            
        } catch (e: Exception) {
            logger.error("❌ Error loading settings, resetting to defaults", e)
            
            // On error, reset to defaults
            val tempState = GitSettingsState()
            XmlSerializerUtil.copyBean(tempState, this.state)
        }
    }

    var githubToken: String
        get() = try {
            PasswordSafe.instance.getPassword(githubTokenCredentials) ?: ""
        } catch (e: Exception) {
            logger.error("❌ Error reading GitHub token from PasswordSafe", e)
            ""
        }
        set(value) {
            try {
                if (value.isBlank()) {
                    PasswordSafe.instance.setPassword(githubTokenCredentials, null)
                } else {
                    PasswordSafe.instance.setPassword(githubTokenCredentials, value)
                }
                logger.info("🔐 GitHub token saved to PasswordSafe")
            } catch (e: Exception) {
                logger.error("❌ Error saving GitHub token to PasswordSafe", e)
            }
        }

    var jiraUrl: String
        get() = state.jiraUrl
        set(value) {
            state.jiraUrl = value
        }

    var jiraEmail: String
        get() = state.jiraEmail
        set(value) {
            state.jiraEmail = value
        }

    var jiraApiToken: String
        get() = try {
            PasswordSafe.instance.getPassword(jiraTokenCredentials) ?: ""
        } catch (e: Exception) {
            logger.error("❌ Error reading JIRA token from PasswordSafe", e)
            ""
        }
        set(value) {
            try {
                if (value.isBlank()) {
                    PasswordSafe.instance.setPassword(jiraTokenCredentials, null)
                } else {
                    PasswordSafe.instance.setPassword(jiraTokenCredentials, value)
                }
                logger.info("🔐 JIRA token saved to PasswordSafe")
            } catch (e: Exception) {
                logger.error("❌ Error saving JIRA token to PasswordSafe", e)
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

    /**
     * Reset all settings to defaults (useful for debugging)
     */
    fun resetToDefaults() {
        logger.info("🔄 Resetting settings to defaults")
        state.resetToDefaults()
        
        // Also clear tokens from PasswordSafe
        try {
            PasswordSafe.instance.setPassword(githubTokenCredentials, null)
            PasswordSafe.instance.setPassword(jiraTokenCredentials, null)
            logger.info("🔐 Cleared tokens from PasswordSafe")
        } catch (e: Exception) {
            logger.error("❌ Error clearing tokens from PasswordSafe", e)
        }
    }

    companion object {
        const val NAME = "GitCommitMessageFormat"
        const val STORAGES = "git-commit-message-format.xml"

        @JvmStatic
        val instance: GitSettings
            get() = service()
    }
}
