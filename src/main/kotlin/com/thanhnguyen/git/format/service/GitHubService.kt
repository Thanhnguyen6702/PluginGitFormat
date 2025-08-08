package com.thanhnguyen.git.format.service

import com.thanhnguyen.git.format.settings.GitSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service(Service.Level.PROJECT)
class GitHubService(private val project: Project) {

    private val logger = Logger.getInstance(GitHubService::class.java)
    // Configure HTTP client to follow redirects automatically and increase timeout
    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()
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
        baseBranch: String,
        assignees: List<String> = emptyList(),
        labels: List<String> = emptyList()
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
            
            // Build assignees and labels JSON arrays
            val assigneesJson = if (assignees.isNotEmpty()) {
                assignees.joinToString(",") { "\"${escapeJsonString(it)}\"" }
            } else ""
            
            val labelsJson = if (labels.isNotEmpty()) {
                labels.joinToString(",") { "\"${escapeJsonString(it)}\"" }
            } else ""
            
            val requestBody = buildString {
                append("{\n")
                append("    \"title\": \"$escapedTitle\",\n")
                append("    \"body\": \"$escapedBody\",\n")
                append("    \"head\": \"$escapedHead\",\n")
                append("    \"base\": \"$escapedBase\"")
                
                if (assigneesJson.isNotEmpty()) {
                    append(",\n    \"assignees\": [$assigneesJson]")
                }
                
                if (labelsJson.isNotEmpty()) {
                    append(",\n    \"labels\": [$labelsJson]")
                }
                
                append("\n}")
            }

            logger.info("🚀 Creating PR for repo: $owner/$repo")
            logger.info("📋 URL: $url")
            logger.info("📄 Head: $headBranch -> Base: $baseBranch")
            if (assignees.isNotEmpty()) {
                logger.info("👤 Assignees: ${assignees.joinToString(", ")}")
            }
            if (labels.isNotEmpty()) {
                logger.info("🏷️ Labels: ${labels.joinToString(", ")}")
            }

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer ${settings.githubToken}")
                .header("Content-Type", "application/json")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "IntelliJ-Plugin-GitCommitFormat/2.2.0")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            logger.info("📤 Response Status: ${response.statusCode()}")
            logger.info("🌐 Final URL: ${response.uri()}")
            
            // Log response headers for debugging redirects
            response.headers().map().forEach { (key, values) ->
                if (key.lowercase().contains("location") || key.lowercase().contains("redirect")) {
                    logger.info("📍 Header $key: ${values.joinToString(", ")}")
                }
            }
            
            when (response.statusCode()) {
                201 -> {
                    // Parse response để lấy URL
                    val responseBody = response.body()
                    val pullRequestUrl = extractPullRequestUrl(responseBody)
                    logger.info("✅ Pull Request đã được tạo thành công: $pullRequestUrl")
                    PullRequestResult(true, pullRequestUrl)
                }
                422 -> {
                    logger.warn("⚠️ Pull Request conflict: ${response.body()}")
                    PullRequestResult(false, error = "Pull Request đã tồn tại hoặc không có thay đổi nào để merge")
                }
                307, 308 -> {
                    // Handle redirect responses explicitly
                    val locationHeader = response.headers().firstValue("Location").orElse(null)
                    logger.error("🔄 Redirect response ${response.statusCode()}: Location = $locationHeader")
                    logger.error("📄 Response body: ${response.body()}")
                    PullRequestResult(false, error = "Redirect error ${response.statusCode()}: ${response.body()}")
                }
                else -> {
                    logger.error("❌ Lỗi tạo Pull Request: ${response.statusCode()}")
                    logger.error("📄 Response body: ${response.body()}")
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
            logger.info("🔍 Parsing repository info from: $remoteUrl")
            
            // Patterns: 
            // https://github.com/owner/repo.git
            // git@github.com:owner/repo.git
            val httpsPattern = Regex("https://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$")
            val sshPattern = Regex("git@github\\.com:([^/]+)/([^/]+?)(?:\\.git)?$")
            
            httpsPattern.find(remoteUrl)?.let { match ->
                val owner = match.groupValues[1]
                val repo = match.groupValues[2]
                logger.info("✅ Parsed HTTPS format: owner=$owner, repo=$repo")
                return Pair(owner, repo)
            }
            
            sshPattern.find(remoteUrl)?.let { match ->
                val owner = match.groupValues[1]
                val repo = match.groupValues[2]
                logger.info("✅ Parsed SSH format: owner=$owner, repo=$repo")
                return Pair(owner, repo)
            }
            
            logger.warn("❌ Could not parse repository info from URL: $remoteUrl")
            null
        } catch (e: Exception) {
            logger.error("💥 Không thể parse repository info từ: $remoteUrl", e)
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
            
            logger.info("🔍 Checking repository access: $owner/$repo")
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer ${settings.githubToken}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "IntelliJ-Plugin-GitCommitFormat/2.2.0")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            logger.info("🔍 Repository check response: ${response.statusCode()}")
            
            when (response.statusCode()) {
                200 -> true
                307, 308 -> {
                    logger.warn("🔄 Repository check redirect ${response.statusCode()}")
                    false
                }
                else -> {
                    logger.warn("❌ Repository access denied or not found: ${response.statusCode()}")
                    false
                }
            }
        } catch (e: Exception) {
            logger.error("💥 Exception khi kiểm tra repository access: ${e.message}", e)
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
     * Lấy username của user hiện tại (authenticated user)
     */
    fun getCurrentUser(): String? {
        if (!settings.isGitHubConfigured()) {
            return null
        }

        return try {
            val url = "https://api.github.com/user"

            logger.info("🔍 Getting current authenticated user")

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer ${settings.githubToken}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "IntelliJ-Plugin-GitCommitFormat/2.2.0")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            logger.info("🔍 User check response: ${response.statusCode()}")

            when (response.statusCode()) {
                200 -> {
                    val responseBody = response.body()
                    val username = extractUsername(responseBody)
                    logger.info("✅ Current user: $username")
                    username
                }
                else -> {
                    logger.warn("❌ Failed to get current user: ${response.statusCode()}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("💥 Exception khi lấy current user: ${e.message}", e)
            null
        }
    }

    private fun extractUsername(responseBody: String): String? {
        return try {
            // Simple regex to extract "login" from JSON response
            val pattern = Regex("\"login\"\\s*:\\s*\"([^\"]+)\"")
            pattern.find(responseBody)?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.error("Không thể extract username từ response", e)
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