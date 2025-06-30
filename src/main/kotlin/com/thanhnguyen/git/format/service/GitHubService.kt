package com.thanhnguyen.git.format.service

import com.thanhnguyen.git.format.settings.GitSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service(Service.Level.PROJECT)
class GitHubService(private val project: Project) {

    private val logger = Logger.getInstance(GitHubService::class.java)
    private val httpClient = HttpClient.newHttpClient()
    private val settings = GitSettings.instance

    data class PullRequestResult(
        val success: Boolean,
        val pullRequestUrl: String? = null,
        val error: String? = null
    )

    /**
     * Tạo Pull Request trên GitHub
     */
    fun createPullRequest(
        owner: String,
        repo: String,
        title: String,
        body: String,
        headBranch: String,
        baseBranch: String
    ): PullRequestResult {
        if (!settings.isGitHubConfigured()) {
            return PullRequestResult(false, error = "GitHub token chưa được cấu hình")
        }

        return try {
            val url = "https://api.github.com/repos/$owner/$repo/pulls"
            
            // Escape JSON strings properly
            val escapedTitle = escapeJsonString(title)
            val escapedBody = escapeJsonString(body)
            val escapedHead = escapeJsonString(headBranch)
            val escapedBase = escapeJsonString(baseBranch)
            
            val requestBody = """
                {
                    "title": "$escapedTitle",
                    "body": "$escapedBody",
                    "head": "$escapedHead",
                    "base": "$escapedBase"
                }
            """.trimIndent()

            logger.info("🚀 Creating PR: $url")
            logger.info("📋 Request body: $requestBody")

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer ${settings.githubToken}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "IntelliJ-Plugin-GitCommitFormat/2.2.0")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            logger.info("📤 Response: ${response.statusCode()}")
            logger.info("📄 Response body: ${response.body()}")
            
            when (response.statusCode()) {
                201 -> {
                    // Parse response để lấy URL
                    val responseBody = response.body()
                    val pullRequestUrl = extractPullRequestUrl(responseBody)
                    logger.info("✅ Pull Request đã được tạo thành công: $pullRequestUrl")
                    PullRequestResult(true, pullRequestUrl)
                }
                422 -> {
                    PullRequestResult(false, error = "Pull Request đã tồn tại hoặc không có thay đổi nào để merge")
                }
                else -> {
                    logger.error("❌ Lỗi tạo Pull Request: ${response.statusCode()} - ${response.body()}")
                    PullRequestResult(false, error = "HTTP ${response.statusCode()}: ${response.body()}")
                }
            }
        } catch (e: Exception) {
            logger.error("💥 Exception khi tạo Pull Request: ${e.message}", e)
            PullRequestResult(false, error = "Exception: ${e.message}")
        }
    }

    /**
     * Lấy thông tin repository từ remote URL
     */
    fun parseRepositoryInfo(remoteUrl: String): Pair<String, String>? {
        return try {
            // Patterns: 
            // https://github.com/owner/repo.git
            // git@github.com:owner/repo.git
            val httpsPattern = Regex("https://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$")
            val sshPattern = Regex("git@github\\.com:([^/]+)/([^/]+?)(?:\\.git)?$")
            
            httpsPattern.find(remoteUrl)?.let { match ->
                return Pair(match.groupValues[1], match.groupValues[2])
            }
            
            sshPattern.find(remoteUrl)?.let { match ->
                return Pair(match.groupValues[1], match.groupValues[2])
            }
            
            null
        } catch (e: Exception) {
            logger.error("Không thể parse repository info từ: $remoteUrl", e)
            null
        }
    }

    /**
     * Kiểm tra xem repository có tồn tại và có quyền truy cập không
     */
    fun checkRepositoryAccess(owner: String, repo: String): Boolean {
        if (!settings.isGitHubConfigured()) {
            return false
        }

        return try {
            val url = "https://api.github.com/repos/$owner/$repo"
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer ${settings.githubToken}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "IntelliJ-Plugin-GitCommitFormat/2.2.0")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            logger.error("Exception khi kiểm tra repository access: ${e.message}", e)
            false
        }
    }

    private fun extractPullRequestUrl(responseBody: String): String? {
        return try {
            // Simple regex to extract "html_url" from JSON response
            val pattern = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"")
            pattern.find(responseBody)?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.error("Không thể extract Pull Request URL từ response", e)
            null
        }
    }

    /**
     * Escape JSON string để tránh lỗi parsing
     */
    private fun escapeJsonString(input: String): String {
        return input
            .replace("\\", "\\\\")  // Escape backslashes first
            .replace("\"", "\\\"")  // Escape quotes
            .replace("\n", "\\n")   // Escape newlines
            .replace("\r", "\\r")   // Escape carriage returns  
            .replace("\t", "\\t")   // Escape tabs
            .replace("\b", "\\b")   // Escape backspace
            .replace("\u000C", "\\f") // Escape form feed
    }
} 