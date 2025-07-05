package com.thanhnguyen.git.format.view

import com.thanhnguyen.git.format.services.PullRequestTemplateService
import com.thanhnguyen.git.format.service.GitBranchService
import com.thanhnguyen.git.format.service.GitHubService
import com.thanhnguyen.git.format.service.JiraService
import com.thanhnguyen.git.format.settings.GitSettings
import com.thanhnguyen.git.format.settings.GitSettingsConfigurable
import com.thanhnguyen.git.format.util.TimeParser
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.application.ReadAction
import com.intellij.ui.dsl.builder.*
import git4idea.GitUtil
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener



class PullRequestDialog(
    private val project: Project,
    private val defaultTitle: String
) : DialogWrapper(project) {
    
    private val logger = Logger.getInstance(PullRequestDialog::class.java)
    private val templateService = PullRequestTemplateService()
    private val branchService = project.getService(GitBranchService::class.java)
    private val githubService = project.getService(GitHubService::class.java)
    private val jiraService = project.getService(JiraService::class.java)
    private val settings = GitSettings.instance
    
    private lateinit var titleField: JTextField
    private lateinit var estimateTimeField: JTextField
    private lateinit var actualTimeField: JTextField
    private lateinit var templateComboBox: JComboBox<PullRequestTemplateService.Template>
    private lateinit var baseBranchComboBox: JComboBox<String>
    private lateinit var previewArea: JTextArea
    private lateinit var statusLabel: JLabel

    private val templates = templateService.getAvailableTemplates(project)
    private val branches = branchService.getRemoteBranches().ifEmpty { 
        listOf(settings.defaultBaseBranch, "develop", "main", "master") 
    }
    
    private var isLoading = false
    private var previewContent: String = ""
    
    private val isValid: Boolean
        get() = try {
            ::titleField.isInitialized && 
            titleField.text.trim().isNotEmpty() && 
            GitSettings.instance.isGitHubConfigured()
        } catch (e: Exception) {
            false
        }
    
    init {
        title = "Create Pull Request & JIRA Integration"
        setOKButtonText("Create Pull Request")
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            group("Pull Request Information") {
                row("Title:") {
                    titleField = textField()
                        .text(defaultTitle)
                        .columns(50)
                        .focused()
                        .applyToComponent {
                            document.addDocumentListener(createDocumentListener())
                            addKeyListener(object : KeyAdapter() {
                                override fun keyPressed(e: KeyEvent) {
                                    if (e.keyCode == KeyEvent.VK_ENTER) {
                                        estimateTimeField.requestFocusInWindow()
                                        e.consume() // Prevent default Enter action
                                    }
                                }
                            })
                        }
                        .component
                }
                
                row("Base Branch:") {
                    baseBranchComboBox = comboBox(branches)
                        .applyToComponent {
                            selectedItem = settings.defaultBaseBranch
                        }
                        .component
                }
                
                row("Template:") {
                    templateComboBox = comboBox(templates)
                        .applyToComponent {
                            renderer = object : DefaultListCellRenderer() {
                                override fun getListCellRendererComponent(
                                    list: JList<*>?, value: Any?, index: Int,
                                    isSelected: Boolean, cellHasFocus: Boolean
                                ): java.awt.Component {
                                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                                    if (value is PullRequestTemplateService.Template) {
                                        text = value.displayName
                                    }
                                    return this
                                }
                            }
                            
                            addActionListener {
                                updatePreview()
                            }
                        }
                        .component
                }
                
                row("Estimate Time:") {
                    estimateTimeField = textField()
                        .columns(20)
                        .applyToComponent {
                            document.addDocumentListener(createDocumentListener())
                            addKeyListener(object : KeyAdapter() {
                                override fun keyPressed(e: KeyEvent) {
                                    if (e.keyCode == KeyEvent.VK_ENTER) {
                                        actualTimeField.requestFocusInWindow()
                                        e.consume() // Prevent default Enter action
                                    }
                                }
                            })
                        }
                        .component
                }
                
                row("Actual Time:") {
                    actualTimeField = textField()
                        .columns(20)
                        .applyToComponent {
                            document.addDocumentListener(createDocumentListener())
                            addKeyListener(object : KeyAdapter() {
                                override fun keyPressed(e: KeyEvent) {
                                    if (e.keyCode == KeyEvent.VK_ENTER) {
                                        okAction.actionPerformed(null)
                                        e.consume() // Prevent default Enter action
                                    }
                                }
                            })
                        }
                        .component
                }
            }
            
            group("Integration Status") {
                row {
                    statusLabel = label(getStatusText()).component
                }
                if (!settings.isGitHubConfigured() || !settings.isJiraConfigured()) {
                    row {
                        link("📝 Cấu hình tại Settings") {
                            openSettings()
                        }
                        comment("Cần cấu hình để sử dụng đầy đủ tính năng")
                    }
                }
            }
            
            group("Pull Request Preview") {
                row {
                    scrollCell(JTextArea().apply {
                        previewArea = this
                        isEditable = true
                        lineWrap = true
                        wrapStyleWord = true
                        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                        background = UIManager.getColor("TextArea.background")
                        border = BorderFactory.createLoweredBevelBorder()
                        isFocusable = true
                        isOpaque = true
                    })
                        .rows(15)
                        .align(Align.FILL)
                }.resizableRow()
            }
        }
    }
    
    private fun createDocumentListener(): DocumentListener {
        return object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updatePreview()
            override fun removeUpdate(e: DocumentEvent?) = updatePreview()
            override fun changedUpdate(e: DocumentEvent?) = updatePreview()
        }
    }
    
    private fun updatePreview() {
        if (isLoading) return
        
        val title = titleField.text.trim()
        val estimateTime = estimateTimeField.text.trim()
        val actualTime = actualTimeField.text.trim()
        
        previewContent = formatPullRequestContent(title, estimateTime, actualTime)
        previewArea.text = previewContent
        previewArea.caretPosition = 0
        
        // Clear existing validation errors first
        setErrorText(null)
        
        // Check validation state
        val validationResult = doValidate()
        val shouldEnable = validationResult == null && !isLoading
        
        // Update button state
        okAction.isEnabled = shouldEnable
        
        // Show validation error if exists
        if (validationResult != null) {
            setErrorText(validationResult.message)
        }
    }
    
    private fun refreshAll() {
        SwingUtilities.invokeLater {
            try {
                // Force refresh settings instance
                val freshSettings = GitSettings.instance
                
                // Update status label
                statusLabel.text = getStatusText()
                
                // Update preview
                updatePreview()
                
                // Clear any existing validation errors
                setErrorText(null)
                
                // Check validation state
                val validationResult = doValidate()
                val shouldEnable = validationResult == null && !isLoading
                
                // Update button state
                okAction.isEnabled = shouldEnable
                
                // Show validation error if exists
                if (validationResult != null) {
                    setErrorText(validationResult.message)
                } else {
                    setErrorText(null)
                }
                
                // Force UI refresh
                statusLabel.parent?.revalidate()
                statusLabel.parent?.repaint()
                
                // Force dialog revalidation
                contentPane?.revalidate()
                contentPane?.repaint()
                
                // Log for debugging
                logger.info("🔄 Dialog refreshed - GitHub configured: ${freshSettings.isGitHubConfigured()}, Button enabled: $shouldEnable, Validation: ${validationResult?.message ?: "OK"}")
                
            } catch (e: Exception) {
                logger.error("❌ Error in refreshAll: ${e.message}", e)
            }
        }
    }
    


    private fun formatPullRequestContent(title: String, estimateTime: String, actualTime: String): String {
        val selectedTemplate = if (::templateComboBox.isInitialized) {
            templateComboBox.selectedItem as? PullRequestTemplateService.Template
        } else null
        
        return if (selectedTemplate != null && templates.isNotEmpty()) {
            try {
                val templateContent = templateService.readTemplate(selectedTemplate, project)
                var result = templateContent
                    .replace("{{title}}", title)
                    .replace("{{estimate_time}}", estimateTime)
                    .replace("{{actual_time}}", actualTime)
                    .replace("{{TITLE}}", title)
                    .replace("{{ESTIMATE_TIME}}", estimateTime)
                    .replace("{{ACTUAL_TIME}}", actualTime)
                
                result = enhanceTemplateWithTimeAndTicket(result, title, estimateTime, actualTime)
                result
            } catch (e: Exception) {
                getDefaultFormattedContent(title, estimateTime, actualTime)
            }
        } else {
            getDefaultFormattedContent(title, estimateTime, actualTime)
        }
    }

    private fun enhanceTemplateWithTimeAndTicket(templateContent: String, title: String, estimateTime: String, actualTime: String): String {
        var result = templateContent

        val patterns = listOf(
            "\\[([A-Z]+\\d+-\\d+)\\]",
            "\\[([A-Z]+-\\d+)\\]",
            "([A-Z]+\\d+-\\d+)",
            "([A-Z]+-\\d+)"
        )

        var fullTicket: String? = null
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(title)
            if (match != null && fullTicket == null) {
                fullTicket = match.groupValues[1]
                break
            }
        }

        if (fullTicket != null && fullTicket.isNotBlank()) {
            val projectPrefix = fullTicket.substringBefore("-")
            val hardcodedProjects = Regex("\\[([A-Z]+\\d*)-\\]").findAll(result).map { it.groupValues[1] }.distinct()

            for (hardcodedProject in hardcodedProjects) {
                result = result.replace("[$hardcodedProject-]", "[$projectPrefix-]")
                result = result.replace("browse/$hardcodedProject-", "browse/$projectPrefix-")
            }

            result = result.replace("[$projectPrefix-]", "[$fullTicket]")
            result = result.replace("browse/$projectPrefix-", "browse/$fullTicket")
        }

        // 2. Handle time sections using TimeParser for proper formatting
        val estimateText = if (estimateTime.isNotBlank()) {
            TimeParser.validateAndFormat(estimateTime) ?: "${estimateTime}m"
        } else ""
        
        val actualText = if (actualTime.isNotBlank()) {
            TimeParser.validateAndFormat(actualTime) ?: "${actualTime}m"
        } else ""

        // First handle Estimate Time
        if (result.contains("# Estimate Time:", ignoreCase = true)) {
            // Template has estimate section, fill it
            if (estimateText.isNotBlank()) {
                result = result.replace(Regex("# Estimate Time:.*"), "# Estimate time: $estimateText")
            }
        }

        // Then handle Actual Time
        if (result.contains("# Actual Time:", ignoreCase = true)) {
            // Template has actual section, fill it
            if (actualText.isNotBlank()) {
                result = result.replace(Regex("# Actual Time:.*"), "# Actual Time: $actualText")
            }
        }

        // Now add missing sections in correct order
        if (!result.contains("# Estimate Time:", ignoreCase = true) && estimateText.isNotBlank()) {
            // Find where to insert estimate time (before actual time or check list)
            if (result.contains("# Actual Time:", ignoreCase = true)) {
                // Insert before actual time with proper spacing
                result = result.replace(Regex("(# Actual Time:.*)"), "# Estimate time: $estimateText\n$1")
            } else if (result.contains("# Check list", ignoreCase = true)) {
                // Insert before check list
                result = result.replace(Regex("(# Check list.*)"), "# Estimate time: $estimateText\n$1")
            } else {
                // Append at end
                result += "\n# Estimate time: $estimateText"
            }
        }

        if (!result.contains("# Actual Time:", ignoreCase = true) && actualText.isNotBlank()) {
            // Find where to insert actual time (after estimate time, before check list)
            if (result.contains("# Check list", ignoreCase = true)) {
                // Insert before check list
                result = result.replace(Regex("(# Check list.*)"), "# Actual Time: $actualText\n$1")
            } else {
                // Append at end
                result += "\n# Actual Time: $actualText"
            }
        }

        return result
    }
    private fun getDefaultFormattedContent(title: String, estimateTime: String, actualTime: String): String {
        val ticketKey = if (title.isNotBlank()) {
            jiraService.extractTicketKey(title)
        } else null

        val resolvedTicketsSection = if (ticketKey != null && ticketKey.isNotBlank()) {
            "# Resolved tickets\n- [$ticketKey](https://apero.atlassian.net/browse/$ticketKey)"
        } else {
            "# Resolved tickets\n- [TICKET-KEY](https://apero.atlassian.net/browse/TICKET-KEY)"
        }

        val estimateText = if (estimateTime.isNotBlank()) {
            TimeParser.validateAndFormat(estimateTime) ?: "${estimateTime}m"
        } else "0m"
        val actualText = if (actualTime.isNotBlank()) {
            TimeParser.validateAndFormat(actualTime) ?: "${actualTime}m"
        } else "0m"

        return """$resolvedTicketsSection
# Estimate time: $estimateText
# Actual Time: $actualText
# Check list
- [ ] Copy/paste PR link into issued tickets""".trimIndent()
    }
    
    override fun doValidate(): ValidationInfo? {
        return try {
            // Ensure fields are initialized
            if (!::titleField.isInitialized) return null
            
            val title = titleField.text.trim()
            val freshSettings = GitSettings.instance
            
            when {
                title.isEmpty() -> ValidationInfo("Title không được để trống", titleField)
                !freshSettings.isGitHubConfigured() -> ValidationInfo("GitHub token chưa được cấu hình trong Settings")
                else -> null
            }
        } catch (e: Exception) {
            logger.error("Error in doValidate: ${e.message}", e)
            ValidationInfo("Validation error", titleField)
        }
    }
    
    override fun doOKAction() {
        if (isLoading) {
            return
        }
        
        val title = titleField.text.trim()
        val estimateTime = estimateTimeField.text.trim()
        val actualTime = actualTimeField.text.trim()
        val baseBranch = baseBranchComboBox.selectedItem as String
        val selectedTemplate = templateComboBox.selectedItem as? PullRequestTemplateService.Template
        
        setLoadingState()
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating Pull Request", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Creating Pull Request..."
                    val result = createPullRequestWithJiraIntegration(selectedTemplate, title, baseBranch, estimateTime, actualTime)
                    
                    SwingUtilities.invokeLater {
                        resetButtonState()
                        if (result != null) {
                            val (pullRequestUrl, ticketKey, currentBranch) = result
                            showSuccessDialog(pullRequestUrl, ticketKey, currentBranch, baseBranch, actualTime)
                        }
                        super@PullRequestDialog.doOKAction()
                    }
                    
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        logger.error("Failed to create pull request", e)
                        Messages.showErrorDialog("Failed to create pull request: ${e.message}", "Error")
                        resetButtonState()
                    }
                }
            }
        })
    }
    
    private fun setLoadingState() {
        isLoading = true
        setOKButtonText("Creating...")
        okAction.isEnabled = false
    }
    
    private fun resetButtonState() {
        isLoading = false
        setOKButtonText("Create Pull Request")
        okAction.isEnabled = isValid
    }
    
    private fun createPullRequestWithJiraIntegration(
        selectedTemplate: PullRequestTemplateService.Template?,
        title: String,
        baseBranch: String,
        estimateTime: String, 
        actualTime: String
    ): Triple<String, String?, String>? {
        // Read git repository info on EDT
        val repoData = ReadAction.compute<Triple<String, String, String>, RuntimeException> {
            val repositories = GitUtil.getRepositoryManager(project).repositories
            if (repositories.isEmpty()) {
                throw RuntimeException("Không tìm thấy Git repository")
            }
            
            val repo = repositories.first()
            val remoteUrl = repo.remotes.firstOrNull()?.firstUrl
            if (remoteUrl == null) {
                throw RuntimeException("Không tìm thấy remote URL")
            }
            
            val repoInfo = githubService.parseRepositoryInfo(remoteUrl)
            if (repoInfo == null) {
                throw RuntimeException("Không thể parse repository info từ: $remoteUrl")
            }
            
            val (owner, repoName) = repoInfo
            val currentBranch = repo.currentBranchName ?: ""
            if (currentBranch.isBlank()) {
                throw RuntimeException("Không thể lấy current branch")
            }
            
            Triple(owner, repoName, currentBranch)
        }
        
        val (owner, repoName, currentBranch) = repoData
        
        val pullRequestBody = if (::previewArea.isInitialized && previewArea.text.isNotBlank()) {
            previewArea.text.trim()
        } else {
            formatPullRequestContent(title, estimateTime, actualTime)
        }
        
        val prResult = githubService.createPullRequest(
            owner = owner,
            repo = repoName,
            title = title,
            body = pullRequestBody,
            headBranch = currentBranch,
            baseBranch = baseBranch
        )
        
        if (prResult.success && prResult.pullRequestUrl != null) {
            // PR created successfully, now handle JIRA integration
            val ticketKey = handleJiraIntegration(title, currentBranch, prResult.pullRequestUrl, actualTime)
            
            return Triple(prResult.pullRequestUrl, ticketKey, currentBranch)
            
        } else {
            throw RuntimeException("Lỗi tạo Pull Request: ${prResult.error}")
        }
    }
    
    private fun handleJiraIntegration(title: String, currentBranch: String, pullRequestUrl: String, actualTime: String): String? {
        if (!settings.isJiraConfigured()) {
            return null
        }
        
        val patterns = listOf(
            "\\[([A-Z]+\\d+-\\d+)\\]",
            "\\[([A-Z]+-\\d+)\\]",
            "([A-Z]+\\d+-\\d+)",
            "([A-Z]+-\\d+)"
        )
        
        var ticketKey: String? = null
        
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(title)
            if (match != null) {
                ticketKey = match.groupValues[1]
                break
            }
        }
        
        if (ticketKey == null) {
            for (pattern in patterns) {
                val regex = Regex(pattern)
                val match = regex.find(currentBranch)
                if (match != null) {
                    ticketKey = match.groupValues[1]
                    break
                }
            }
        }
        
        if (ticketKey != null) {
            jiraService.commentOnTicket(ticketKey, pullRequestUrl)
            
            // Use TimeParser to convert actualTime to minutes for JIRA logging
            val actualMinutes = if (actualTime.isNotBlank()) {
                TimeParser.parseToMinutes(actualTime)
            } else null
            
            if (actualMinutes != null && actualMinutes > 0) {
                jiraService.logWork(
                    ticketKey, 
                    actualMinutes, 
                    "Development work - Pull Request: $pullRequestUrl"
                )
            }
        }
        
        return ticketKey
    }
    
    private fun showSuccessDialog(pullRequestUrl: String, ticketKey: String?, currentBranch: String, baseBranch: String, actualTime: String) {
        val jiraUrl = if (ticketKey != null) "https://apero.atlassian.net/browse/$ticketKey" else null
        
        val dialog = object : DialogWrapper(project) {
            init {
                title = "Pull Request Created Successfully"
                setOKButtonText("Close")
                init()
            }
            
            override fun createCenterPanel(): JComponent {
                return panel {
                    row {
                        label("🎉 Pull Request đã được tạo thành công!").apply {
                            component.font = component.font.deriveFont(16f)
                        }
                    }
                    
                    separator()
                    
                    group("Pull Request Information") {
                        row("📋 URL:") {
                            text(pullRequestUrl)
                                .applyToComponent { 
                                    toolTipText = pullRequestUrl
                                }
                            button("📋 Copy") {
                                copyToClipboard(pullRequestUrl)
                                showNotification("PR URL copied to clipboard!")
                            }
                        }
                        
                        row("🌿 Branch:") {
                            text("$currentBranch → $baseBranch")
                        }
                    }
                    
                    if (ticketKey != null && settings.isJiraConfigured()) {
                        group("JIRA Integration") {
                            row("🎫 Ticket:") {
                                text(ticketKey)
                                button("📋 Copy") {
                                    copyToClipboard(ticketKey)
                                    showNotification("JIRA ticket copied!")
                                }
                                button("🔗 Open") {
                                    if (jiraUrl != null) {
                                        openUrl(jiraUrl)
                                    }
                                }
                            }
                            
                            row("💬 Status:") {
                                text("✅ PR link đã được comment vào JIRA")
                            }
                            
                            if (actualTime.isNotBlank() && TimeParser.parseToMinutes(actualTime) != null) {
                                val actualMinutes = TimeParser.parseToMinutes(actualTime)!!
                                val formattedTime = TimeParser.formatMinutes(actualMinutes)
                                row("⏰ Work Log:") {
                                    text("✅ Đã log $formattedTime ($actualMinutes phút) vào JIRA")
                                }
                            }
                        }
                    } else if (!settings.isJiraConfigured()) {
                        group("JIRA Integration") {
                            row {
                                text("ℹ️ JIRA chưa được cấu hình")
                            }
                        }
                    } else {
                        group("JIRA Integration") {
                            row {
                                text("⚠️ Không tìm thấy JIRA ticket trong title")
                            }
                        }
                    }
                }
            }
        }
        
        dialog.show()
    }
    
    private fun copyToClipboard(text: String) {
        try {
            CopyPasteManager.getInstance().setContents(StringSelection(text))
        } catch (e: Exception) {
            // Fallback to system clipboard
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
        }
    }
    
    private fun openUrl(url: String) {
        try {
            val desktop = java.awt.Desktop.getDesktop()
            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                desktop.browse(java.net.URI(url))
            }
        } catch (e: Exception) {
            logger.error("Failed to open URL: $url", e)
        }
    }
    
    private fun showNotification(message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Git Commit Format")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }
    
    override fun getPreferredFocusedComponent(): JComponent? {
        return titleField
    }
    
    override fun dispose() {
        super.dispose()
    }
    
    override fun show() {
        isLoading = false
        
        if (templates.isNotEmpty()) {
            templateComboBox.selectedItem = templates.first()
        }
        
        SwingUtilities.invokeLater {
            refreshAll()
        }
        
        super.show()
    }
    
    private fun getStatusText(): String {
        val githubStatus = if (settings.isGitHubConfigured()) "✅ GitHub" else "❌ GitHub (cần config token)"
        val jiraStatus = if (settings.isJiraConfigured()) "✅ JIRA" else "❌ JIRA (cần config)"
        return "$githubStatus | $jiraStatus"
    }
    
    private fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, GitSettingsConfigurable::class.java)
        
        // Immediate refresh
        refreshAll()
        
        // Use Timer to delay refresh and allow settings to be properly saved
        val timer = Timer(200) { // 200ms delay
            refreshAll()
            
            // Final refresh to ensure UI is updated
            Timer(100) { 
                refreshAll() 
            }.apply { 
                isRepeats = false 
                start() 
            }
        }
        timer.isRepeats = false
        timer.start()
    }
} 