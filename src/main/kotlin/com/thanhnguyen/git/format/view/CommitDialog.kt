package com.thanhnguyen.git.format.view

import com.thanhnguyen.git.format.domain.CommitMessage
import com.thanhnguyen.git.format.services.ProjectPrefixService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.*
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class CommitDialog(
    private val project: Project,
    private val commitMessageEditor: Any?
) : DialogWrapper(project) {
    
    private lateinit var issueNumberField: JTextField
    private lateinit var descriptionField: JTextField
    private lateinit var previewArea: JTextArea
    
    private val commitMessage = CommitMessage()
    private val prefixService = project.getService(ProjectPrefixService::class.java)
    
    init {
        title = "Create Commit Message"
        // Lấy project prefix từ template
        commitMessage.projectPrefix = prefixService.getProjectPrefix(project)
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            group("Commit Information") {
                row("Issue Number:") {
                    issueNumberField = textField()
                        .bindText(commitMessage::issueNumber)
                        .columns(15)
                        .focused()
                        .applyToComponent {
                            document.addDocumentListener(createDocumentListener())
                            addKeyListener(object : KeyAdapter() {
                                override fun keyPressed(e: KeyEvent) {
                                    if (e.keyCode == KeyEvent.VK_ENTER) {
                                        descriptionField.requestFocus()
                                        e.consume()
                                    }
                                }
                            })
                        }
                        .component
                }
                
                row("Description:") {
                    descriptionField = textField()
                        .bindText(commitMessage::description)
                        .columns(50)
                        .applyToComponent {
                            document.addDocumentListener(createDocumentListener())
                            addKeyListener(object : KeyAdapter() {
                                override fun keyPressed(e: KeyEvent) {
                                    if (e.keyCode == KeyEvent.VK_ENTER) {
                                        // Focus vào nút OK
                                        val okAction = getOKAction()
                                        if (okAction.isEnabled) {
                                            okAction.actionPerformed(null)
                                        }
                                        e.consume()
                                    }
                                }
                            })
                        }
                        .component
                }
            }
            
            group("Preview") {
                row {
                    scrollCell(JTextArea().apply {
                        previewArea = this
                        isEditable = false
                        lineWrap = true
                        wrapStyleWord = true
                        font = Font(Font.MONOSPACED, Font.PLAIN, 14)
                        background = UIManager.getColor("TextField.background")
                        border = BorderFactory.createLoweredBevelBorder()
                    })
                        .rows(3)
                        .align(Align.FILL)
                }.resizableRow()
            }
        }
    }
    
    private fun createDocumentListener(): javax.swing.event.DocumentListener {
        return object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) {
                updatePreview()
            }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) {
                updatePreview()
            }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) {
                updatePreview()
            }
        }
    }

    private fun updatePreview() {
        SwingUtilities.invokeLater {
            // Update commit message from UI
            commitMessage.issueNumber = issueNumberField.text.trim()
            commitMessage.description = descriptionField.text.trim()
            
            // Update preview ngay khi có bất kỳ thay đổi nào
            val preview = commitMessage.formatCommitMessage()
            
            previewArea.text = preview
            previewArea.caretPosition = 0
        }
    }
    
    override fun getPreferredFocusedComponent(): JComponent? {
        return issueNumberField
    }
    
    override fun show() {
        updatePreview()
        super.show()
    }
    
    fun getFormattedMessage(): String {
        return commitMessage.formatCommitMessage()
    }
} 