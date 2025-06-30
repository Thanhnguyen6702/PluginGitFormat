package com.thanhnguyen.git.format.domain

data class CommitMessage(
    var issueNumber: String = "",
    var description: String = "",
    var projectPrefix: String = "AIP"
) {
    fun formatCommitMessage(): String {
        val issue = if (issueNumber.isNotBlank()) issueNumber else "{issue_number}"
        val desc = if (description.isNotBlank()) description else "{description}"
        
        return "[$projectPrefix-$issue] $desc"
    }
    
    fun isValid(): Boolean {
        return issueNumber.isNotBlank() && description.isNotBlank()
    }
} 