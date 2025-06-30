package com.thanhnguyen.git.format.util

import com.thanhnguyen.git.format.constant.GitConstant

/**
 * String Utils
 *
 * @author Thanh Nguyen
 * @since 2.0.11
 */
object StringUtils {

    @JvmStatic
    fun formatClosedIssue(closedIssue: String): String {
        val issue = closedIssue.trim()
        val issueNum = issue.trim('#').trim()
        return when {
            isNumeric(issue) -> "#$issue"
            isNumeric(issueNum) -> "#$issueNum"
            else -> GitConstant.EMPTY
        }
    }

    @JvmStatic
    fun isNumeric(str: String?): Boolean {
        if (str.isNullOrEmpty()) {
            return false
        }
        return str.all { it.isDigit() }
    }

    @JvmStatic
    fun isBlank(str: String?): Boolean {
        if (str.isNullOrEmpty()) {
            return true
        }
        return str.isBlank()
    }
}