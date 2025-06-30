package com.thanhnguyen.git.format.settings

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField

/**
 * Git Settings Configurable - Simplified for GitHub and JIRA Integration
 */
internal class GitSettingsConfigurable : BoundSearchableConfigurable(
    "Git Commit Format & Pull Request",
    "git.commit.message"
) {
    private val state: GitSettings get() = GitSettings.instance

    override fun createPanel(): DialogPanel {
        return panel {
            group("GitHub Configuration") {
                row("GitHub Token:") {
                    val tokenField = JBPasswordField()
                    tokenField.text = state.githubToken
                    cell(tokenField)
                        .bindText(state::githubToken)
                        .columns(COLUMNS_LARGE)
                        .comment("Personal Access Token để tạo Pull Request trên GitHub")
                }
                row("Default Base Branch:") {
                    textField()
                        .bindText(state::defaultBaseBranch)
                        .columns(COLUMNS_MEDIUM)
                        .comment("Nhánh mặc định để tạo Pull Request (vd: develop, main)")
                }
            }

            group("JIRA Configuration") {
                row("JIRA URL:") {
                    textField()
                        .bindText(state::jiraUrl)
                        .columns(COLUMNS_LARGE)
                        .comment("URL JIRA instance của bạn (vd: https://company.atlassian.net)")
                }
                row("JIRA Email:") {
                    textField()
                        .bindText(state::jiraEmail)
                        .columns(COLUMNS_LARGE)
                        .comment("Email đăng nhập JIRA")
                }
                row("JIRA API Token:") {
                    val jiraTokenField = JBPasswordField()
                    jiraTokenField.text = state.jiraApiToken
                    cell(jiraTokenField)
                        .bindText(state::jiraApiToken)
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

    override fun apply() {
        super.apply()
        // Settings are automatically saved to PasswordSafe through property setters
        // No additional action needed as BoundSearchableConfigurable handles the binding
    }
}
