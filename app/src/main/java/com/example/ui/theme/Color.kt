package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary PDF Crimson Palette
val PdfPrimary = Color(0xFFE11D48) // Modern Rose-Crimson
val PdfPrimaryVariant = Color(0xFFBE123C) // Deep Crimson
val PdfSecondary = Color(0xFF1E293B) // Slate 800
val PdfSecondaryVariant = Color(0xFF0F172A) // Slate 900
val PdfAccent = Color(0xFFF59E0B) // Warm Amber

// Vibrant Feature Accents
val ToolCoral = Color(0xFFF43F5E)
val ToolIndigo = Color(0xFF6366F1)
val ToolEmerald = Color(0xFF10B981)
val ToolAmber = Color(0xFFF59E0B)

// Luxury Gradients
val HeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFE11D48), Color(0xFF8B5CF6))
)
val CardGradient1 = Brush.linearGradient(
    colors = listOf(Color(0xFFFFF1F2), Color(0xFFFFFFFF))
)
val CardBorderGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFE11D48).copy(alpha = 0.4f), Color(0xFF6366F1).copy(alpha = 0.2f))
)

// Light Mode Surfaces
val LightBackground = Color(0xFFF8FAFC) // Clean Crisp Slate 50
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurface = Color(0xFF0F172A)
val LightOutline = Color(0xFFE2E8F0)

// Dark Mode Surfaces
val DarkBackground = Color(0xFF0B1120) // Deep Dark Navy
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkOutline = Color(0xFF475569)

// Feature Badge Colors
val GreenSuccess = Color(0xFF10B981)
val BlueInfo = Color(0xFF3B82F6)
val PurpleBadge = Color(0xFF8B5CF6)
