package com.thanhnguyen.git.format.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import git4idea.GitUtil
import git4idea.repo.GitRepository

@Service(Service.Level.PROJECT)
class GitBranchService(private val project: Project) {

    fun getAllBranches(): List<String> {
        return try {
            val repositories = GitUtil.getRepositoryManager(project).repositories
            if (repositories.isEmpty()) {
                emptyList()
            } else {
                val repo = repositories.first()
                getAllBranchesFromRepo(repo)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getRemoteBranches(): List<String> {
        return try {
            val repositories = GitUtil.getRepositoryManager(project).repositories
            if (repositories.isEmpty()) {
                emptyList()
            } else {
                val repo = repositories.first()
                getRemoteBranchesFromRepo(repo)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCurrentBranch(): String {
        return try {
            val repositories = GitUtil.getRepositoryManager(project).repositories
            if (repositories.isEmpty()) {
                ""
            } else {
                val repo = repositories.first()
                repo.currentBranchName ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun getAllBranchesFromRepo(repo: GitRepository): List<String> {
        val branches = mutableListOf<String>()
        
        // Add local branches
        repo.branches.localBranches.forEach { branch ->
            branches.add(branch.name)
        }
        
        // Add remote branches (without origin/ prefix)
        repo.branches.remoteBranches.forEach { branch ->
            val branchName = branch.nameForRemoteOperations
            if (!branches.contains(branchName)) {
                branches.add(branchName)
            }
        }
        
        return branches.sorted()
    }

    private fun getRemoteBranchesFromRepo(repo: GitRepository): List<String> {
        return repo.branches.remoteBranches.map { branch ->
            branch.nameForRemoteOperations
        }.sorted()
    }
} 