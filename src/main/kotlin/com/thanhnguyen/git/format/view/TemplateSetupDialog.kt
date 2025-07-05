package com.thanhnguyen.git.format.view

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.dsl.builder.*
import java.awt.Font
import java.io.File
import javax.swing.*

class TemplateSetupDialog(
    private val project: Project
) : DialogWrapper(project) {
    
    private val logger = Logger.getInstance(TemplateSetupDialog::class.java)
    
    private lateinit var projectCodeField: JTextField
    private lateinit var createDefaultTemplatesCheckbox: JCheckBox
    private lateinit var previewArea: JTextArea
    
    private var setupResult: TemplateSetupResult? = null
    
    data class TemplateSetupResult(
        val projectCode: String,
        val createDefaultTemplates: Boolean
    )
    
    init {
        title = "Setup Pull Request Templates"
        setOKButtonText("Create Templates")
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            group("Project Configuration") {
                row("Mã Project:") {
                    projectCodeField = textField()
                        .columns(30)
                        .focused()
                        .applyToComponent {
                            toolTipText = "Nhập mã project của bạn (VD: AIP339, MyProject, ABC-123, ...)"
                            document.addDocumentListener(object : javax.swing.event.DocumentListener {
                                override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updatePreview()
                                override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updatePreview()
                                override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updatePreview()
                            })
                        }
                        .component
                }
                
                row {
                    createDefaultTemplatesCheckbox = checkBox("Tạo templates mặc định (bug_fix.md, feature_request.md)")
                        .selected(true)
                        .applyToComponent {
                            addActionListener { updatePreview() }
                        }
                        .component
                }
                
                row {
                    comment("Templates sẽ được tạo trong thư mục .github/PULL_REQUEST_TEMPLATE/")
                }
            }
            
            group("Preview Template") {
                row {
                    scrollCell(JTextArea().apply {
                        previewArea = this
                        isEditable = false
                        lineWrap = true
                        wrapStyleWord = true
                        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                        background = UIManager.getColor("TextArea.disabledBackground")
                        border = BorderFactory.createLoweredBevelBorder()
                    })
                        .rows(12)
                        .align(Align.FILL)
                }.resizableRow()
            }
        }
    }
    
    private fun updatePreview() {
        val projectCode = projectCodeField.text.trim().uppercase()
        
        if (projectCode.isBlank()) {
            previewArea.text = "Nhập mã project để xem preview..."
            return
        }
        
        val preview = if (createDefaultTemplatesCheckbox.isSelected) {
            """Templates sẽ được tạo:

📁 .github/PULL_REQUEST_TEMPLATE/
├── bug_fix.md
└── feature_request.md

=== bug_fix.md ===
# Resolved tickets
- [$projectCode-](https://jira.apero.vn/browse/$projectCode-)

# Checklist
- [ ] I have checked the previous pull request.

=== feature_request.md ===
# Resolved tickets  
- [$projectCode-](https://jira.apero.vn/browse/$projectCode-)

# Checklist
- [ ] I have checked the previous pull request.
- [ ] Added tests for new feature
- [ ] Updated documentation"""
        } else {
            """Template cơ bản sẽ được tạo:

📁 .github/PULL_REQUEST_TEMPLATE/
└── pull_request_template.md

=== pull_request_template.md ===
# Resolved tickets
- [$projectCode-](https://jira.apero.vn/browse/$projectCode-)

# Checklist
- [ ] I have checked the previous pull request."""
        }
        
        previewArea.text = preview
        previewArea.caretPosition = 0
    }
    
    override fun doValidate(): ValidationInfo? {
        return try {
            val projectCode = projectCodeField.text.trim()
            
            when {
                projectCode.isBlank() -> ValidationInfo("Mã project không được để trống", projectCodeField)
                else -> null
            }
        } catch (e: Exception) {
            logger.error("Error in doValidate: ${e.message}", e)
            ValidationInfo("Validation error", projectCodeField)
        }
    }
    
    override fun doOKAction() {
        val validation = doValidate()
        if (validation != null) {
            return
        }
        
        val projectCode = projectCodeField.text.trim().uppercase()
        val createDefaults = createDefaultTemplatesCheckbox.isSelected
        
        try {
            createTemplateFiles(projectCode, createDefaults)
            setupResult = TemplateSetupResult(projectCode, createDefaults)
            super.doOKAction()
        } catch (e: Exception) {
            logger.error("Failed to create templates: ${e.message}", e)
            setErrorText("Lỗi tạo templates: ${e.message}")
        }
    }
    
    private fun createTemplateFiles(projectCode: String, createDefaults: Boolean) {
        val projectPath = project.basePath ?: throw RuntimeException("Không thể xác định project path")
        val templatesDir = File(projectPath, ".github/PULL_REQUEST_TEMPLATE")
        
        // Tạo thư mục templates
        if (!templatesDir.exists()) {
            templatesDir.mkdirs()
            logger.info("📁 Created templates directory: ${templatesDir.path}")
        }
        
        if (createDefaults) {
            // Tạo bug_fix.md
            val bugFixContent = """# Resolved tickets
- [$projectCode-](https://jira.apero.vn/browse/$projectCode-)

# Checklist
- [ ] I have checked the previous pull request."""

            File(templatesDir, "bug_fix.md").writeText(bugFixContent)
            logger.info("📄 Created bug_fix.md")

            // Tạo feature_request.md
            val featureContent = """# Resolved tickets
- [$projectCode-](https://jira.apero.vn/browse/$projectCode-)

# Checklist
- [ ] I have checked the previous pull request.
- [ ] Added tests for new feature
- [ ] Updated documentation"""

            File(templatesDir, "feature_request.md").writeText(featureContent)
            logger.info("📄 Created feature_request.md")

        } else {
            // Tạo template cơ bản
            val basicContent = """# Resolved tickets
- [$projectCode-](https://jira.apero.vn/browse/$projectCode-)

# Checklist
- [ ] I have checked the previous pull request."""

            File(templatesDir, "pull_request_template.md").writeText(basicContent)
            logger.info("📄 Created pull_request_template.md")
        }
    }
    
    fun getSetupResult(): TemplateSetupResult? = setupResult
    
    override fun getPreferredFocusedComponent(): JComponent? {
        return projectCodeField
    }
    
    override fun show() {
        SwingUtilities.invokeLater {
            updatePreview()
        }
        super.show()
    }
}