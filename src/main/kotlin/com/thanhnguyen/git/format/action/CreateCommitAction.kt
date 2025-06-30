package com.thanhnguyen.git.format.action

import com.thanhnguyen.git.format.view.CommitDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.ui.CommitMessage

class CreateCommitAction : AnAction(), DumbAware {
    
    private val logger = Logger.getInstance(CreateCommitAction::class.java)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        logger.info("🎯 CreateCommitAction: Opening commit message format dialog...")
        
        try {
            // Get commit message editor if available
            val commitMessageEditor = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
            
            // Open commit dialog
            val dialog = CommitDialog(project, commitMessageEditor)
            if (dialog.showAndGet()) {
                val formattedMessage = dialog.getFormattedMessage()
                
                // Set formatted message to commit editor if available
                commitMessageEditor?.let { editor ->
                    if (editor is CommitMessage) {
                        editor.setCommitMessage(formattedMessage)
                    }
                }
                
                logger.info("✅ Commit message formatted: $formattedMessage")
            }
            
        } catch (ex: Exception) {
            logger.error("❌ Failed to open commit message dialog", ex)
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }
} 