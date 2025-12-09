package com.bsbeetle.bananaspaghetti.utils.git

/**
 * Git Data Classes and Result Types
 *
 * This file contains all data classes and sealed classes used throughout the Git system.
 * Centralizing these types makes them easier to maintain and ensures consistency.
 *
 * Key types:
 * - GitResult: Wrapper for operation results with success/error states
 * - BranchInfo: Represents a Git branch with metadata
 * - CommitInfo: Represents a Git commit with author and message
 * - StashInfo: Represents a stashed change
 */

// ============================================================================
// Result Wrapper
// ============================================================================

/**
 * Result wrapper for Git operations. Provides consistent error handling across all Git functions.
 *
 * Use Success for successful operations with optional data and message. Use Error for failures with
 * a user-friendly message and optional technical details.
 *
 * Example:
 * ```kotlin
 * when (val result = repository.commit("message")) {
 *     is GitResult.Success -> showSuccess(result.message)
 *     is GitResult.Error -> showError(result.message, result.details)
 * }
 * ```
 */
sealed class GitResult<out T> {
    /**
     * Successful operation result.
     * @param data The result data (Unit if no data needed)
     * @param message User-friendly success message
     */
    data class Success<T>(val data: T, val message: String = "") : GitResult<T>()

    /**
     * Failed operation result.
     * @param message User-friendly error message
     * @param details Optional technical details for debugging
     */
    data class Error(val message: String, val details: String? = null) : GitResult<Nothing>()
}

// ============================================================================
// Branch Information
// ============================================================================

/**
 * Represents a Git branch with its metadata.
 *
 * @param name Branch name (e.g., "main", "feature/login")
 * @param isRemote True if this is a remote branch (e.g., "origin/main")
 * @param isCurrent True if this is the currently checked out branch
 *
 * Example:
 * ```kotlin
 * val branch = BranchInfo(
 *     name = "main",
 *     isRemote = false,
 *     isCurrent = true
 * )
 * ```
 */
data class BranchInfo(val name: String, val isRemote: Boolean, val isCurrent: Boolean)

// ============================================================================
// Commit Information
// ============================================================================

/**
 * Represents a Git commit with full metadata.
 *
 * @param hash Short commit hash (e.g., "a1b2c3d")
 * @param shortHash Duplicate of hash for compatibility
 * @param message Commit message (first line)
 * @param fullMessage Complete commit message with body
 * @param author Author name
 * @param email Author email
 * @param date Commit date as Java Date object
 *
 * Example:
 * ```kotlin
 * val commit = CommitInfo(
 *     hash = "a1b2c3d",
 *     shortHash = "a1b2c3d",
 *     message = "Add login feature",
 *     fullMessage = "Add login feature\n\nImplemented OAuth2...",
 *     author = "John Doe",
 *     email = "john@example.com",
 *     date = Date()
 * )
 * ```
 */
data class CommitInfo(
        val hash: String,
        val shortHash: String,
        val message: String,
        val fullMessage: String,
        val author: String,
        val email: String,
        val date: java.util.Date
)

// ============================================================================
// Stash Information
// ============================================================================

/**
 * Represents a stashed change in Git.
 *
 * Stashes allow you to save uncommitted changes temporarily and restore them later.
 *
 * @param index Stash index (0 is most recent)
 * @param message Stash message/description
 * @param date Date when stash was created
 *
 * Example:
 * ```kotlin
 * val stash = StashInfo(
 *     index = 0,
 *     message = "WIP: working on feature",
 *     date = Date()
 * )
 * ```
 */
data class StashInfo(val index: Int, val message: String, val date: java.util.Date)
