package com.thanhnguyen.git.format.action

import com.thanhnguyen.git.format.view.PullRequestDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vcs.VcsDataKeys
import java.io.File

class CreatePullRequestAction : AnAction("Create Pull Request", "Create pull request with template and time tracking", null), DumbAware {
    
    private val logger = Logger.getInstance(CreatePullRequestAction::class.java)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        logger.info("🎯 CreatePullRequestAction: Opening pull request dialog...")
        
        try {
            // Get last commit message
            val lastCommitMessage = getLastCommitMessage(project)
            logger.info("📝 Last commit: $lastCommitMessage")
            
            // Open pull request dialog
            val dialog = PullRequestDialog(project, lastCommitMessage)
            dialog.show()
            
        } catch (ex: Exception) {
            logger.error("❌ Failed to open pull request dialog", ex)
        }
    }
    
    private fun getLastCommitMessage(project: com.intellij.openapi.project.Project): String {
        return try {
            val projectPath = project.basePath ?: return "Default commit message"
            val gitDir = File(projectPath, ".git")
            
            if (gitDir.exists()) {
                // Try to get last commit message from git log
                val processBuilder = ProcessBuilder("git", "log", "-1", "--pretty=format:%s")
                processBuilder.directory(File(projectPath))
                
                val process = processBuilder.start()
                val result = process.inputStream.bufferedReader().readText().trim()
                
                process.waitFor()
                
                if (result.isNotEmpty()) {
                    logger.info("✅ Got last commit message: $result")
                    result
                } else {
                    "Default commit message"
                }
            } else {
                logger.warn("⚠️ No git repository found")
                "Default commit message"
            }
        } catch (e: Exception) {
            logger.warn("❌ Failed to get last commit message: ${e.message}")
            "Default commit message"
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val hasProject = project != null
        
        e.presentation.isEnabledAndVisible = hasProject
    }
} 