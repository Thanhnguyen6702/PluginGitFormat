package com.thanhnguyen.git.format.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.regex.Pattern

class PullRequestTemplateService {

    private val logger = thisLogger()

    data class Template(
        val name: String,
        val displayName: String,
        val fileName: String,
        val ticketPattern: String?
    )

    data class TemplateInfo(
        val name: String,
        val displayName: String,
        val ticketPattern: String?
    )

    enum class TemplateType {
        MAIN, BUG_FIX, FEATURE_REQUEST, BUILD_MAIN
    }

    private val ticketPatternRegex = Pattern.compile("\\[([A-Z]+\\d+-?)\\]")

    /**
     * Get all available pull request templates
     */
    fun getAvailableTemplates(project: Project): List<Template> {
        val templates = mutableListOf<Template>()
        
        try {
            val projectPath = project.basePath ?: return emptyList()
            val templatesDir = File(projectPath, ".github/PULL_REQUEST_TEMPLATE")
        
            if (!templatesDir.exists() || !templatesDir.isDirectory) {
                logger.info("📁 Templates directory not found: ${templatesDir.path}")
            return emptyList()
        }

            templatesDir.listFiles { file ->
                file.isFile && file.name.endsWith(".md")
            }?.forEach { file ->
                val name = file.nameWithoutExtension
                val displayName = formatDisplayName(name)
                val ticketPattern = extractTicketPattern(file)
                
                templates.add(Template(name, displayName, file.name, ticketPattern))
                logger.info("📋 Found template: $displayName (${file.name})")
            }
                    } catch (e: Exception) {
            logger.warn("❌ Error reading templates: ${e.message}")
        }

        return templates.sortedBy { it.displayName }
    }

    /**
     * Read template content from file
     */
    fun readTemplate(template: Template, project: Project): String {
        return try {
            val projectPath = project.basePath ?: return getDefaultTemplateContent()
            val templateFile = File(projectPath, ".github/PULL_REQUEST_TEMPLATE/${template.fileName}")
            
            if (templateFile.exists()) {
                val content = templateFile.readText()
                logger.info("📄 Read template content from: ${template.fileName}")
                content
            } else {
                logger.warn("⚠️ Template file not found: ${templateFile.path}")
                getDefaultTemplateContent()
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to read template: ${e.message}")
            getDefaultTemplateContent()
        }
    }

    private fun getDefaultTemplateContent(): String {
        return """
## Pull Request: {{title}}

### Time Tracking
- **Estimate:** {{estimate_time}}
- **Actual:** {{actual_time}}

### Description
Brief description of changes

### Type of Change
- [ ] Bug fix
- [ ] New feature  
- [ ] Breaking change
- [ ] Documentation update

### Testing
- [ ] Tests pass locally
- [ ] Added new tests for changes

### Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated if needed
        """.trimIndent()
    }

    private fun formatDisplayName(name: String): String {
        return name.split("_")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
    
    private fun extractTicketPattern(file: File): String? {
        try {
            val content = file.readText()
            val matcher = ticketPatternRegex.matcher(content)
            
            if (matcher.find()) {
                return matcher.group(1) // Returns "AIP698-" from "[AIP698-]"
            }
        } catch (e: Exception) {
            // Ignore and return null
        }
        return null
    }
    
    fun formatTicketNumber(template: Template, ticketNumber: String): String {
        return if (template.ticketPattern != null && ticketNumber.isNotBlank()) {
            "[${template.ticketPattern}${ticketNumber}]"
        } else {
            ""
        }
    }

    /**
     * Get the default template
     */
    fun getDefaultTemplate(project: Project): Template? {
        return getAvailableTemplates(project).firstOrNull()
    }
}