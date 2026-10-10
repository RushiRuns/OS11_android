package com.os11.domain.logic.validation

/**
 * Dependency cycle check.
 *
 * In OS11, task_dependencies has (task_id, depends_on_id), meaning task_id depends on depends_on_id.
 * Adding a dependency (A, B) creates a cycle if B already depends (transitively) on A, or if A == B.
 */
object DependencyCycleCheck {

    /**
     * Checks if adding dependency (taskId depends on dependsOnId) would introduce a cycle.
     *
     * @param existingEdges list of (task, dependsOn) pairs
     * @param taskId task that wants to depend on dependsOnId
     * @param dependsOnId task being depended upon
     * @return true if a cycle would be created, false otherwise
     */
    fun wouldCreateCycle(
        existingEdges: Collection<Pair<String, String>>,
        taskId: String,
        dependsOnId: String,
    ): Boolean {
        if (taskId == dependsOnId) return true

        // Build adjacency list: node -> set of nodes it depends on
        val graph = mutableMapOf<String, MutableSet<String>>()
        for ((u, v) in existingEdges) {
            graph.computeIfAbsent(u) { mutableSetOf() }.add(v)
        }
        // Add proposed edge
        graph.computeIfAbsent(taskId) { mutableSetOf() }.add(dependsOnId)

        // Run cycle detection from taskId
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        fun dfs(node: String): Boolean {
            visited.add(node)
            recursionStack.add(node)

            val neighbors = graph[node] ?: emptySet()
            val hasCycle = neighbors.any { neighbor ->
                neighbor in recursionStack || (neighbor !in visited && dfs(neighbor))
            }

            recursionStack.remove(node)
            return hasCycle
        }

        return dfs(taskId)
    }
}

/**
 * Parent cycle check for subtasks (parent_task_id).
 */
object ParentCycleCheck {

    /**
     * Checks if setting parent of [taskId] to [newParentId] would introduce a cycle.
     *
     * @param taskId task whose parent is being updated
     * @param newParentId target parent ID, or null if moving to root
     * @param getParent lookup function from child ID to current parent ID
     * @return true if setting parent would create a cycle, false otherwise
     */
    fun wouldCreateCycle(taskId: String, newParentId: String?, getParent: (String) -> String?): Boolean {
        if (newParentId == null) return false
        var current: String? = newParentId
        val visited = mutableSetOf<String>()
        var cycleDetected = taskId == newParentId

        while (current != null && !cycleDetected) {
            if (current == taskId || !visited.add(current)) {
                cycleDetected = true
            } else {
                current = getParent(current)
            }
        }
        return cycleDetected
    }
}
