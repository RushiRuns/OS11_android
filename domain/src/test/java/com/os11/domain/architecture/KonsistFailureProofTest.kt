package com.os11.domain.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spike S-6: Proof that Konsist architecture tests fail when rules are violated.
 *
 * Verifies that:
 * 1. Importing MaterialTheme.* in a feature package fails assertions.
 * 2. Importing Room DAOs in UI packages fails assertions.
 * 3. Using raw Color(0x...) literals in UI outside ui.theme fails assertions.
 */
class KonsistFailureProofTest {

    private val fixtureScope = Konsist.scopeFromDirectory(
        "domain/src/test/resources/fixtures/architecture_violations",
    )

    @Test
    fun prove_architecture_test_fails_when_feature_code_imports_MaterialTheme() {
        val result = runCatching {
            fixtureScope
                .files
                .withPackage("com.os11.app.ui.features..")
                .assertFalse { file ->
                    file.imports.any { it.name.startsWith("androidx.compose.material3.MaterialTheme") }
                }
        }
        assertTrue(
            "Expected failure when feature code imports MaterialTheme",
            result.isFailure,
        )
    }

    @Test
    fun prove_architecture_test_fails_when_UI_code_imports_DAO() {
        val result = runCatching {
            fixtureScope
                .files
                .withPackage("com.os11.app.ui..")
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains(".data.db.") || import.name.contains(".data.repository.")
                    }
                }
        }
        assertTrue(
            "Expected failure when UI code imports DAO",
            result.isFailure,
        )
    }

    @Test
    fun prove_architecture_test_fails_when_UI_code_uses_raw_color_literals() {
        val colorHexRegex = Regex("""Color\s*\(\s*0x[0-9a-fA-F]+""", RegexOption.IGNORE_CASE)
        val result = runCatching {
            fixtureScope
                .files
                .withPackage("com.os11.app.ui..")
                .assertFalse { file ->
                    val isTheme = file.hasPackage("com.os11.app.ui.theme..")
                    !isTheme && colorHexRegex.containsMatchIn(file.text)
                }
        }
        assertTrue(
            "Expected failure when UI code uses raw Color literals",
            result.isFailure,
        )
    }
}
