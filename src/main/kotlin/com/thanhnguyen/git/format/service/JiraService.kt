package com.thanhnguyen.git.format.service

import com.thanhnguyen.git.format.settings.GitSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.*

@Service(Service.Level.PROJECT)
class JiraService(private val project: Project) {

    private val logger = Logger.getInstance(JiraService::class.java)
    private val httpClient = HttpClient.newHttpClient()
    private val settings = GitSettings.instance

    /**
     * Comment Pull Request link vào JIRA ticket
     */
    fun commentOnTicket(ticketKey: String, pullRequestUrl: String): Boolean {
        logger.info("🎫 Attempting to comment on JIRA ticket: $ticketKey")
        logger.info("🔗 PR URL: $pullRequestUrl")
        
        if (!settings.isJiraConfigured()) {
            logger.warn("❌ JIRA chưa được cấu hình")
            logger.warn("   JIRA URL: '${settings.jiraUrl}'")
            logger.warn("   JIRA Email: '${settings.jiraEmail}'")
            logger.warn("   JIRA Token: '${if (settings.jiraApiToken.isBlank()) "EMPTY" else "SET"}'")
            return false
        }

        return try {
            val url = "${settings.jiraUrl}/rest/api/3/issue/$ticketKey/comment"
            
            logger.info("🚀 JIRA Comment URL: $url")
            
            val requestBody = """
                {
                    "body": {
                        "type": "doc",
                        "version": 1,
                        "content": [
                            {
                                "type": "paragraph",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "🔗 Pull Request đã được tạo: "
                                    },
                                    {
                                        "type": "text",
                                        "text": "$pullRequestUrl",
                                        "marks": [
                                            {
                                                "type": "link",
                                                "attrs": {
                                                    "href": "$pullRequestUrl"
                                                }
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                }
            """.trimIndent()

            logger.info("📋 Request body: $requestBody")

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic ${getBasicAuth()}")
                .header("Content-Type", "application/json")
                .header("User-Agent", "IntelliJ-Plugin-GitCommitFormat/2.2.0")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            logger.info("📤 JIRA Response: ${response.statusCode()}")
            logger.info("📄 Response body: ${response.body()}")
            
            if (response.statusCode() in 200..299) {
                logger.info("✅ Đã comment Pull Request vào JIRA ticket $ticketKey")
                true
            } else {
                logger.error("❌ Lỗi comment vào JIRA: ${response.statusCode()} - ${response.body()}")
                false
            }
        } catch (e: Exception) {
            logger.error("💥 Exception khi comment vào JIRA: ${e.message}", e)
            false
        }
    }

    /**
     * Log work vào JIRA ticket
     */
    fun logWork(ticketKey: String, timeSpentMinutes: Int, description: String = "Development work"): Boolean {
        logger.info("⏱️ Attempting to log work on JIRA ticket: $ticketKey")
        logger.info("⏰ Time: $timeSpentMinutes minutes")
        logger.info("📝 Description: $description")
        
        if (!settings.isJiraConfigured()) {
            logger.warn("❌ JIRA chưa được cấu hình cho log work")
            return false
        }
        
        if (timeSpentMinutes <= 0) {
            logger.warn("❌ Invalid time: $timeSpentMinutes minutes")
            return false
        }

        return try {
            val url = "${settings.jiraUrl}/rest/api/3/issue/$ticketKey/worklog"
            
            logger.info("🚀 JIRA Log Work URL: $url")
            
            val pullRequestUrl = description.substringAfter("Pull Request: ").trim()
            val hasValidUrl = pullRequestUrl.startsWith("http")
            
            val requestBody = if (hasValidUrl) {
                """
                {
                    "timeSpentSeconds": ${timeSpentMinutes * 60},
                    "comment": {
                        "type": "doc",
                        "version": 1,
                        "content": [
                            {
                                "type": "paragraph",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "Development work - Pull Request: "
                                    },
                                    {
                                        "type": "text",
                                        "text": "$pullRequestUrl",
                                        "marks": [
                                            {
                                                "type": "link",
                                                "attrs": {
                                                    "href": "$pullRequestUrl"
                                                }
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()
            } else {
                """
                {
                    "timeSpentSeconds": ${timeSpentMinutes * 60},
                    "comment": {
                        "type": "doc",
                        "version": 1,
                        "content": [
                            {
                                "type": "paragraph",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": "$description"
                                    }
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()
            }

            logger.info("📋 Log Work body: $requestBody")

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic ${getBasicAuth()}")
                .header("Content-Type", "application/json")
                .header("User-Agent", "IntelliJ-Plugin-GitCommitFormat/2.2.0")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            logger.info("📤 Log Work Response: ${response.statusCode()}")
            logger.info("📄 Response body: ${response.body()}")
            
            if (response.statusCode() in 200..299) {
                logger.info("✅ Đã log work ${timeSpentMinutes} phút vào JIRA ticket $ticketKey")
                true
            } else {
                logger.error("❌ Lỗi log work vào JIRA: ${response.statusCode()} - ${response.body()}")
                false
            }
        } catch (e: Exception) {
            logger.error("💥 Exception khi log work vào JIRA: ${e.message}", e)
            false
        }
    }

    /**
     * Kiểm tra ticket có tồn tại không
     */
    fun ticketExists(ticketKey: String): Boolean {
        if (!settings.isJiraConfigured()) {
            return false
        }

        return try {
            val url = "${settings.jiraUrl}/rest/api/3/issue/$ticketKey"
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic ${getBasicAuth()}")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            logger.error("Exception khi kiểm tra ticket JIRA: ${e.message}", e)
            false
        }
    }

    /**
     * Extract JIRA ticket key từ title hoặc branch name
     */
    fun extractTicketKey(text: String): String? {
        // Pattern cho JIRA ticket: ABC-123, TEST-456, etc.
        val pattern = Regex("[A-Z]+-\\d+")
        return pattern.find(text)?.value
    }

    private fun getBasicAuth(): String {
        val credentials = "${settings.jiraEmail}:${settings.jiraApiToken}"
        return Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
    }
} 