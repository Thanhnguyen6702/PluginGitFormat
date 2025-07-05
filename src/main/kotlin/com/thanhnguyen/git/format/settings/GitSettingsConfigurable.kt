package com.thanhnguyen.git.format.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.application.ApplicationManager
import javax.swing.JComponent

internal class GitSettingsConfigurable : SearchableConfigurable {
    private val state: GitSettings get() = GitSettings.instance
    
    // UI components
    private lateinit var githubTokenField: JBPasswordField
    private lateinit var jiraUrlField: JBTextField
    private lateinit var jiraEmailField: JBTextField
    private lateinit var jiraTokenField: JBPasswordField
    private lateinit var defaultBranchField: JBTextField

    override fun getId(): String = "git.commit.message"
    override fun getDisplayName(): String = "Git Commit Format & Pull Request"

    override fun createComponent(): JComponent {
        // Initialize UI components
        githubTokenField = JBPasswordField()
        jiraUrlField = JBTextField()
        jiraEmailField = JBTextField()
        jiraTokenField = JBPasswordField()
        defaultBranchField = JBTextField()

        return panel {
            group("GitHub Configuration") {
                row("GitHub Token:") {
                    cell(githubTokenField)
                        .columns(COLUMNS_LARGE)
                        .comment("Personal Access Token để tạo Pull Request trên GitHub")
                }
                row("Default Base Branch:") {
                    cell(defaultBranchField)
                        .columns(COLUMNS_MEDIUM)
                        .comment("Nhánh mặc định để tạo Pull Request (vd: develop, main)")
                }
            }

            group("JIRA Configuration") {
                row("JIRA URL:") {
                    cell(jiraUrlField)
                        .columns(COLUMNS_LARGE)
                        .comment("URL JIRA instance của bạn (vd: https://company.atlassian.net)")
                }
                row("JIRA Email:") {
                    cell(jiraEmailField)
                        .columns(COLUMNS_LARGE)
                        .comment("Email đăng nhập JIRA")
                }
                row("JIRA API Token:") {
                    cell(jiraTokenField)
                        .columns(COLUMNS_LARGE)
                        .comment("API Token từ JIRA Account Settings > Security > API tokens")
                }
            }

            group("Hướng dẫn") {
                row {
                    comment("""
                        <b>GitHub Token:</b><br>
                        • Vào GitHub Settings > Developer settings > Personal access tokens<br>
                        • Tạo token mới với quyền: repo, pull_request<br><br>
                        
                        <b>JIRA API Token:</b><br>
                        • Vào JIRA Account Settings > Security > API tokens<br>
                        • Tạo token mới cho plugin này<br><br>
                        
                        <b>Chức năng:</b><br>
                        • Plugin sẽ tự động comment link Pull Request vào JIRA ticket<br>
                        • Log work với thời gian actual time đã nhập
                    """.trimIndent())
                }
            }

            group("Token Security") {
                row {
                    comment("""
                        <b>🔐 Bảo mật Token:</b><br>
                        • Tokens được lưu trữ an toàn bằng IntelliJ PasswordSafe<br>
                        • Không được lưu trong file cấu hình plain text<br>
                        • Tự động mã hóa theo OS credentials store
                    """.trimIndent())
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return githubTokenField.text != state.githubToken ||
               jiraUrlField.text != state.jiraUrl ||
               jiraEmailField.text != state.jiraEmail ||
               jiraTokenField.text != state.jiraApiToken ||
               defaultBranchField.text != state.defaultBaseBranch
    }

    override fun apply() {
        // Apply changes from UI to state
        state.githubToken = githubTokenField.text
        state.jiraUrl = jiraUrlField.text
        state.jiraEmail = jiraEmailField.text
        state.jiraApiToken = jiraTokenField.text
        state.defaultBaseBranch = defaultBranchField.text
        
        // Save settings
        ApplicationManager.getApplication().saveSettings()
    }

    override fun reset() {
        // Load values from state to UI
        githubTokenField.text = state.githubToken
        jiraUrlField.text = state.jiraUrl
        jiraEmailField.text = state.jiraEmail
        jiraTokenField.text = state.jiraApiToken
        defaultBranchField.text = state.defaultBaseBranch
    }
}
