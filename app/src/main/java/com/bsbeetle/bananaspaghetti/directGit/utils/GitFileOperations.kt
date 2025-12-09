package com.bsbeetle.bananaspaghetti.utils.git

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator

/**
 * Git File Operations
 *
 * Handles all file-related Git operations including staging, unstaging, committing, and viewing
 * diffs.
 *
 * This class is extracted from GitRepository to keep file operations separate from branch and
 * network operations, making the code more maintainable and easier to understand.
 *
 * Key responsibilities:
 * - Stage individual files or multiple files for commit
 * - Unstage files (remove from staging area)
 * - Create commits with messages
 * - Generate diffs to show file changes
 * - Reset staging area (mixed reset)
 *
 * Dependencies:
 * - JGit library for Git operations
 * - GitResult for consistent error handling
 * - GitLogger for operation logging
 *
 * Example usage:
 * ```kotlin
 * val fileOps = GitFileOperations(git, rootDir)
 *
 * // Stage a file
 * fileOps.stageFile("src/Main.kt")
 *
 * // Commit changes
 * fileOps.commit("Add new feature")
 *
 * // View diff
 * val diff = fileOps.getDiff("src/Main.kt")
 * ```
 */
class GitFileOperations(private val git: Git?, private val rootDir: File) {

    // ========================================================================
    // Staging Operations
    // ========================================================================

    /**
     * Stages a single file for commit.
     *
     * Adds the file to Git's staging area (index). The file must be relative to the repository root
     * or an absolute path within the repo.
     *
     * @param filePath Absolute or relative path to the file
     * @return Success if staged, Error otherwise
     *
     * Example:
     * ```kotlin
     * val result = stageFile("/path/to/repo/src/Main.kt")
     * when (result) {
     *     is GitResult.Success -> println("Staged successfully")
     *     is GitResult.Error -> println("Error: ${result.message}")
     * }
     * ```
     */
    fun stageFile(filePath: String): GitResult<Unit> {
        val relativePath = toRelativePath(filePath)
        GitLogger.log("git add $relativePath", LogType.COMMAND)
        return try {
            git?.add()?.addFilepattern(relativePath)?.call()
            GitLogger.log("Staged: $relativePath", LogType.SUCCESS)
            GitResult.Success(Unit, "Staged $relativePath")
        } catch (e: Exception) {
            Log.e("GitFileOps", "Failed to stage file", e)
            GitLogger.log("Failed to stage: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to stage file", e.message)
        }
    }

    /**
     * Stages multiple files at once.
     *
     * More efficient than calling stageFile() multiple times. Useful when committing multiple
     * changes together.
     *
     * @param paths List of file paths (absolute or relative)
     * @return Success if all staged, Error otherwise
     *
     * Example:
     * ```kotlin
     * val files = listOf("src/Main.kt", "src/Utils.kt")
     * stageFiles(files)
     * ```
     */
    fun stageFiles(paths: List<String>): GitResult<Unit> {
        GitLogger.log("git add ${paths.size} files", LogType.COMMAND)
        return try {
            val addCommand = git?.add()
            paths.forEach { path ->
                val relativePath = toRelativePath(path)
                addCommand?.addFilepattern(relativePath)
            }
            addCommand?.call()
            GitLogger.log("Staged ${paths.size} files", LogType.SUCCESS)
            GitResult.Success(Unit, "Staged ${paths.size} files")
        } catch (e: Exception) {
            Log.e("GitFileOps", "Failed to stage files", e)
            GitLogger.log("Failed to stage files: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to stage files", e.message)
        }
    }

    /**
     * Stages all modified and untracked files.
     *
     * Equivalent to `git add .` - stages everything in the working directory. Use with caution as
     * it stages all changes.
     *
     * @return Success if staged, Error otherwise
     *
     * Example:
     * ```kotlin
     * stageAll() // Stages all changes
     * ```
     */
    fun stageAll(): GitResult<Unit> {
        GitLogger.log("git add .", LogType.COMMAND)
        return try {
            git?.add()?.addFilepattern(".")?.call()
            GitLogger.log("Staged all changes", LogType.SUCCESS)
            GitResult.Success(Unit, "Staged all changes")
        } catch (e: Exception) {
            Log.e("GitFileOps", "Failed to stage all", e)
            GitLogger.log("Failed to stage all: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to stage all", e.message)
        }
    }

    /**
     * Unstages a file (removes from staging area).
     *
     * The file remains modified in the working directory but is removed from the staging area.
     * Equivalent to `git reset HEAD <file>`.
     *
     * @param filePath Path to the file to unstage
     * @return Success if unstaged, Error otherwise
     *
     * Example:
     * ```kotlin
     * unstageFile("src/Main.kt") // Remove from staging
     * ```
     */
    fun unstageFile(filePath: String): GitResult<Unit> {
        val relativePath = toRelativePath(filePath)
        GitLogger.log("git reset HEAD $relativePath", LogType.COMMAND)
        return try {
            git?.reset()?.addPath(relativePath)?.call()
            GitLogger.log("Unstaged: $relativePath", LogType.SUCCESS)
            GitResult.Success(Unit, "Unstaged $relativePath")
        } catch (e: Exception) {
            Log.e("GitFileOps", "Failed to unstage file", e)
            GitLogger.log("Failed to unstage: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to unstage file", e.message)
        }
    }

    /**
     * Resets the staging area (mixed reset).
     *
     * Removes all files from staging but keeps changes in working directory. Equivalent to `git
     * reset HEAD` - undoes all staging.
     *
     * @return Success if reset, Error otherwise
     *
     * Example:
     * ```kotlin
     * resetMixed() // Clear staging area
     * ```
     */
    fun resetMixed(): GitResult<Unit> {
        GitLogger.log("git reset HEAD", LogType.COMMAND)
        return try {
            git?.reset()?.call()
            GitLogger.log("Reset staging area", LogType.SUCCESS)
            GitResult.Success(Unit, "Reset staging area")
        } catch (e: Exception) {
            Log.e("GitFileOps", "Failed to reset", e)
            GitLogger.log("Failed to reset: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to reset", e.message)
        }
    }

    // ========================================================================
    // Commit Operations
    // ========================================================================

    /**
     * Creates a commit with the staged changes.
     *
     * Commits all files in the staging area with the provided message. The commit author is taken
     * from Git config (user.name and user.email).
     *
     * @param message Commit message describing the changes
     * @return Success with commit hash, Error otherwise
     *
     * Example:
     * ```kotlin
     * val result = commit("Add login feature")
     * when (result) {
     *     is GitResult.Success -> println("Committed: ${result.data}")
     *     is GitResult.Error -> println("Failed: ${result.message}")
     * }
     * ```
     */
    fun commit(message: String): GitResult<String> {
        GitLogger.log("git commit -m \"$message\"", LogType.COMMAND)
        return try {
            val commit = git?.commit()?.setMessage(message)?.call()
            val hash = commit?.name?.substring(0, 7) ?: "unknown"
            GitLogger.log("Committed $hash", LogType.SUCCESS)
            GitResult.Success(hash, "Committed $hash")
        } catch (e: org.eclipse.jgit.api.errors.NoHeadException) {
            // First commit in repo - this is normal
            try {
                val commit = git?.commit()?.setMessage(message)?.setAllowEmpty(false)?.call()
                val hash = commit?.name?.substring(0, 7) ?: "unknown"
                GitLogger.log("Initial commit $hash", LogType.SUCCESS)
                GitResult.Success(hash, "Initial commit $hash")
            } catch (e2: Exception) {
                Log.e("GitFileOps", "Failed to create initial commit", e2)
                GitLogger.log("Commit failed: ${e2.message}", LogType.ERROR)
                GitResult.Error("Failed to commit", e2.message)
            }
        } catch (e: Exception) {
            Log.e("GitFileOps", "Failed to commit", e)
            GitLogger.log("Commit failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to commit", e.message)
        }
    }

    // ========================================================================
    // Diff Operations
    // ========================================================================

    /**
     * Gets the diff for a specific file.
     *
     * Shows the changes between the working directory and the last commit. Returns a unified diff
     * format showing additions (+) and deletions (-).
     *
     * @param filePath Path to the file
     * @return Success with diff text, Error otherwise
     *
     * Example:
     * ```kotlin
     * val diff = getDiff("src/Main.kt")
     * // Returns something like:
     * // @@ -10,3 +10,4 @@
     * //  fun main() {
     * // +    println("Hello")
     * //  }
     * ```
     */
    fun getDiff(filePath: String): GitResult<String> {
        val relativePath = toRelativePath(filePath)
        return try {
            val repository = git?.repository ?: return GitResult.Error("No repository")
            val diffFormatter = DiffFormatter(ByteArrayOutputStream())
            diffFormatter.setRepository(repository)

            // Get HEAD tree
            val head = repository.resolve("HEAD^{tree}")
            val oldTreeIter =
                    if (head != null) {
                        CanonicalTreeParser().apply {
                            repository.newObjectReader().use { reader -> reset(reader, head) }
                        }
                    } else {
                        EmptyTreeIterator()
                    }

            // Get working tree
            val newTreeIter =
                    git.diff().call().firstOrNull()?.newPath?.let { _ -> CanonicalTreeParser() }
                            ?: EmptyTreeIterator()

            val diffs =
                    git.diff()
                            .setOldTree(oldTreeIter)
                            .setPathFilter(
                                    org.eclipse.jgit.treewalk.filter.PathFilter.create(relativePath)
                            )
                            .call()

            val output = ByteArrayOutputStream()
            val formatter = DiffFormatter(output)
            formatter.setRepository(repository)
            diffs.forEach { formatter.format(it) }

            GitResult.Success(output.toString(), "Diff retrieved")
        } catch (e: Exception) {
            Log.e("GitFileOps", "Failed to get diff", e)
            GitResult.Error("Failed to get diff", e.message)
        }
    }

    // ========================================================================
    // Helper Functions
    // ========================================================================

    /**
     * Converts an absolute file path to a path relative to the repository root.
     *
     * Git operations require paths relative to the repo root. This function handles both absolute
     * and already-relative paths.
     *
     * @param filePath Absolute or relative file path
     * @return Path relative to repository root
     *
     * Example:
     * ```kotlin
     * // If rootDir = /storage/emulated/0/myrepo
     * toRelativePath("/storage/emulated/0/myrepo/src/Main.kt")
     * // Returns: "src/Main.kt"
     * ```
     */
    private fun toRelativePath(filePath: String): String {
        val file = File(filePath)
        return if (file.isAbsolute) {
            file.relativeTo(rootDir).path
        } else {
            filePath
        }
    }
}
