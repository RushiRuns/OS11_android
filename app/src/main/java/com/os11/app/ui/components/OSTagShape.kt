package com.os11.app.ui.components

/**
 * Geometric shape variants for tags ensuring accessibility and color-blind safety.
 *
 * Rules per ARCHITECTURE.md Rule 265:
 * Color is never the only signal; shape variants carry over so color-blind users
 * can distinguish tags by geometric silhouette.
 */
enum class OSTagShape(val label: String) {
    Circle("Circle"),
    Square("Square"),
    Triangle("Triangle"),
    Diamond("Diamond"),
    ;

    companion object {
        /**
         * Resolves a deterministic shape for a tag name and color if no explicit shape is stored.
         */
        fun fromTag(name: String, colorHex: String? = null): OSTagShape {
            val hash = (name.hashCode() xor (colorHex?.hashCode() ?: 0)).let { if (it < 0) -it else it }
            val shapes = entries
            return shapes[hash % shapes.size]
        }
    }
}
