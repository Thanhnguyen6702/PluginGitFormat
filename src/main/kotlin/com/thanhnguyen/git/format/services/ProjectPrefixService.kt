package com.thanhnguyen.git.format.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

@Service
class ProjectPrefixService {
    
    fun getProjectPrefix(project: Project): String {
        return try {
            val projectPath = project.basePath ?: return "AIP"
            
            // Thử đọc từ bug_fix.md trước
            val bugFixPath = File(projectPath, ".github/PULL_REQUEST_TEMPLATE/bug_fix.md")
            if (bugFixPath.exists()) {
                val prefix = extractPrefixFromTemplate(bugFixPath.readText())
                if (prefix != null) return prefix
            }
            
            // Nếu không có thì thử feature_request.md
            val featurePath = File(projectPath, ".github/PULL_REQUEST_TEMPLATE/feature_request.md")
            if (featurePath.exists()) {
                val prefix = extractPrefixFromTemplate(featurePath.readText())
                if (prefix != null) return prefix
            }
            
            "AIP" // Fallback
        } catch (e: Exception) {
            "AIP" // Fallback khi có lỗi
        }
    }
    
    private fun extractPrefixFromTemplate(content: String): String? {
        // Tìm pattern [XXX-] hoặc [XXX123-] trong template
        val regex = Regex("\\[([A-Z]+\\d*)-\\]")
        val match = regex.find(content)
        return match?.groupValues?.get(1)
    }
} 