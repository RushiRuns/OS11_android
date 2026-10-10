package com.os11.domain.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

/**
 * Architecture rules enforced via Konsist.
 *
 * Rules per ARCHITECTURE.md and SYSTEM.md:
 * - Pure domain: No android or androidx imports in :domain module.
 * - Concurrency: No GlobalScope, no runBlocking in production code.
 * - UI layers: No MaterialTheme.* in feature packages (tokens only).
 * - Separation: UI composables never import repositories or DAOs.
 */
class KonsistArchitectureTest {
    @Test
    fun no_android_dependencies_in_domain_module() {
        Konsist
            .scopeFromProduction("domain")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("android.") || import.name.startsWith("androidx.")
                }
            }
    }

    @Test
    fun no_GlobalScope_in_production_code() {
        Konsist
            .scopeFromProduction()
            .files
            .assertFalse { file ->
                file.imports.any { it.name == "kotlinx.coroutines.GlobalScope" }
            }
    }

    @Test
    fun no_runBlocking_in_production_code() {
        Konsist
            .scopeFromProduction()
            .files
            .assertFalse { file ->
                file.imports.any { it.name == "kotlinx.coroutines.runBlocking" }
            }
    }

    @Test
    fun feature_code_never_imports_MaterialTheme() {
        Konsist
            .scopeFromProduction("app")
            .files
            .withPackage("com.os11.app.ui.features..")
            .assertFalse { file ->
                file.imports.any { it.name.startsWith("androidx.compose.material3.MaterialTheme") }
            }
    }

    @Test
    fun composables_never_import_repositories_or_DAOs() {
        Konsist
            .scopeFromProduction("app")
            .files
            .withPackage("com.os11.app.ui..")
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains(".data.db.") || import.name.contains(".data.repository.")
                }
            }
    }
}
