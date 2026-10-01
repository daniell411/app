package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core brand palette (APUNTA)
val ApuntaIndigo = Color(0xFF4F46E5)
val ApuntaIndigoLight = Color(0xFF6366F1)
val ApuntaIndigoDark = Color(0xFF3730A3)
val ApuntaOrange = Color(0xFFF59E0B)
val ApuntaGreen = Color(0xFF10B981)
val ApuntaCoral = Color(0xFFEF4444)

// Neutral theme backgrounds
val ApuntaLightBg = Color(0xFFFAF6EE)
val ApuntaLightSurface = Color(0xFFFFFFFF)
val ApuntaLightSurfaceVariant = Color(0xFFF3ECE0)
val ApuntaLightText = Color(0xFF1F2937)
val ApuntaLightTextSecondary = Color(0xFF6B7280)
val ApuntaLightBorder = Color(0xFFE5DECE)

val ApuntaDarkBg = Color(0xFF12151C)
val ApuntaDarkSurface = Color(0xFF1C2230)
val ApuntaDarkSurfaceVariant = Color(0xFF283145)
val ApuntaDarkText = Color(0xFFF3F4F6)
val ApuntaDarkTextSecondary = Color(0xFF9CA3AF)
val ApuntaDarkBorder = Color(0xFF2E384D)

// 8 Palette colors for Notebooks
val NotebookIndigo = Color(0xFF4F46E5)
val NotebookOrange = Color(0xFFF59E0B)
val NotebookGreen = Color(0xFF10B981)
val NotebookPink = Color(0xFFEC4899)
val NotebookCyan = Color(0xFF06B6D4)
val NotebookPurple = Color(0xFF8B5CF6)
val NotebookYellow = Color(0xFFEAB308)
val NotebookCoral = Color(0xFFF43F5E)

val NotebookPalette = listOf(
    "#4F46E5" to NotebookIndigo,
    "#F59E0B" to NotebookOrange,
    "#10B981" to NotebookGreen,
    "#EC4899" to NotebookPink,
    "#06B6D4" to NotebookCyan,
    "#8B5CF6" to NotebookPurple,
    "#EAB308" to NotebookYellow,
    "#F43F5E" to NotebookCoral
)

fun parseHexColor(hex: String, default: Color = ApuntaIndigo): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else if (clean.length == 8) {
            Color(colorInt)
        } else {
            default
        }
    } catch (_: Exception) {
        default
    }
}
