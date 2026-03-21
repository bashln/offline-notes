# Implementation Plan: Obsidianite Theme for OfflineNotes

## Overview
Add the "Obsidianite" theme palette and apply Obsidianite-specific styling to Org mode preview while maintaining backward compatibility with existing themes and Markdown preview.

---

## Files to Modify

### 1. ThemeSettings.kt
**Location:** `app/src/main/java/com/offlinenotes/ui/theme/ThemeSettings.kt`

**Changes:**
- Add `Obsidianite` entry to `ThemePalette` enum

```kotlin
enum class ThemePalette(
    val storageValue: String,
    val displayName: String
) {
    TokyoNight("tokyo_night", "Tokyo Night"),
    Catppuccin("catppuccin", "Catppuccin"),
    RosePine("rose_pine", "Rose Pine"),
    Obsidianite("obsidianite", "Obsidianite");  // NEW

    companion object {
        fun fromStorageValue(value: String?): ThemePalette {
            return entries.firstOrNull { it.storageValue == value } ?: TokyoNight
        }
    }
}
```

---

### 2. Color.kt
**Location:** `app/src/main/java/com/offlinenotes/ui/theme/Color.kt`

**Changes:**

#### A. Add Obsidianite color constants (after existing color definitions, before PaletteTokens)

```kotlin
// Obsidianite Colors
val ObsidianiteBackground = Color(0xFF100E17)
val ObsidianiteBackgroundAlt = Color(0xFF0D0B12)
val ObsidianiteSurface = Color(0xFF191621)
val ObsidianiteSurfaceAlt = Color(0xFF0D0B12)
val ObsidianitePrimary = Color(0xFF0FB6D6)          // Cyan accent
val ObsidianiteOnPrimary = Color(0xFF100E17)
val ObsidianiteTextNormal = Color(0xFFBEBEBE)
val ObsidianiteTextFaint = Color(0xFF7AA2F7)        // Blue-ish faint
val ObsidianiteTextAccent = Color(0xFF0FB6D6)       // Cyan
val ObsidianiteTextSubAccent = Color(0xFFF4569D)    // Pink
val ObsidianiteTextDim = Color(0xFF45AAFF)          // Dim blue
val ObsidianiteTextLink = Color(0xFF6BCAFB)         // Link blue
val ObsidianiteTextMark = Color(0xFF263D92)         // Highlight
val ObsidianiteInteractiveAccent = Color(0x800ED2F7) // Cyan 50%
val ObsidianiteBorder = Color(0x0D0ED2F7)           // Cyan 5%
val ObsidianiteBlockquoteBorder = Color(0xFF4AA8FB)
val ObsidianiteTableBorder = Color(0x260ED2F7)      // Cyan 15%
val ObsidianiteOutline = Color(0xFF6E7681)
val ObsidianiteError = Color(0xFFF4569D)            // Pink for errors
```

#### B. Add Obsidianite PaletteTokens (after rosePineLight, before offlineColorScheme)

```kotlin
private val obsidianiteDark = PaletteTokens(
    primary = ObsidianitePrimary,
    onPrimary = ObsidianiteOnPrimary,
    background = ObsidianiteBackground,
    onBackground = ObsidianiteTextNormal,
    surface = ObsidianiteSurface,
    onSurface = ObsidianiteTextNormal,
    surfaceVariant = ObsidianiteSurfaceAlt,
    onSurfaceVariant = ObsidianiteTextFaint,
    outline = ObsidianiteOutline,
    error = ObsidianiteError
)

// Light variant: fallback to dark (Obsidianite is primarily dark)
// Or create a simplified light variant if desired
private val obsidianiteLight = obsidianiteDark
```

#### C. Update offlineColorScheme() function

```kotlin
fun offlineColorScheme(
    palette: ThemePalette,
    darkTheme: Boolean
): ColorScheme {
    val tokens = when (palette) {
        ThemePalette.TokyoNight -> if (darkTheme) tokyoNightDark else tokyoNightLight
        ThemePalette.Catppuccin -> if (darkTheme) catppuccinDark else catppuccinLight
        ThemePalette.RosePine -> if (darkTheme) rosePineDark else rosePineLight
        ThemePalette.Obsidianite -> obsidianiteDark // Always use dark for Obsidianite
    }
    // ... rest of function unchanged
}
```

---

### 3. NotePreview.kt
**Location:** `app/src/main/java/com/offlinenotes/ui/editor/NotePreview.kt`

**Changes:** This is the most significant set of changes. We need to:
1. Accept `ThemePalette` as a parameter to conditionally apply Obsidianite styling
2. Create Obsidianite-specific composables for Org preview
3. Modify `NotePreviewContent` to route to Obsidianite variants when applicable

#### A. Add imports and preview tokens

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import com.offlinenotes.ui.theme.ThemePalette
import com.offlinenotes.ui.theme.ObsidianiteBackground
import com.offlinenotes.ui.theme.ObsidianiteSurface
import com.offlinenotes.ui.theme.ObsidianiteTextAccent
import com.offlinenotes.ui.theme.ObsidianiteTextDim
import com.offlinenotes.ui.theme.ObsidianiteTextFaint
import com.offlinenotes.ui.theme.ObsidianiteTextLink
import com.offlinenotes.ui.theme.ObsidianiteTextNormal
import com.offlinenotes.ui.theme.ObsidianiteTextSubAccent
import com.offlinenotes.ui.theme.ObsidianiteBorder
import com.offlinenotes.ui.theme.ObsidianiteInteractiveAccent
```

#### B. Update NotePreviewContent signature and logic

```kotlin
@Composable
fun NotePreviewContent(
    text: String,
    isOrg: Boolean,
    palette: ThemePalette = ThemePalette.TokyoNight,  // NEW parameter
    modifier: Modifier = Modifier
) {
    val blocks = parsePreviewBlocks(text, isOrg)
    val isObsidianite = palette == ThemePalette.Obsidianite && isOrg  // NEW
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(blocks) { _, block ->
            when (block) {
                is PreviewBlock.Heading -> {
                    if (isObsidianite) {
                        ObsidianiteHeading(block = block)
                    } else {
                        StandardHeading(block = block, isOrg = isOrg)
                    }
                }
                is PreviewBlock.Checklist -> {
                    if (isObsidianite) {
                        ObsidianiteChecklist(block = block)
                    } else {
                        StandardChecklist(block = block, isOrg = isOrg)
                    }
                }
                is PreviewBlock.Bullet -> {
                    if (isObsidianite) {
                        ObsidianiteBullet(block = block)
                    } else {
                        StandardBullet(block = block, isOrg = isOrg)
                    }
                }
                is PreviewBlock.CodeBlock -> {
                    if (isObsidianite) {
                        ObsidianiteCodeBlock(block = block)
                    } else {
                        StandardCodeBlock(block = block)
                    }
                }
                is PreviewBlock.Paragraph -> {
                    if (isObsidianite) {
                        ObsidianiteParagraph(block = block)
                    } else {
                        StandardParagraph(block = block, isOrg = isOrg)
                    }
                }
                PreviewBlock.Empty -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
```

#### C. Extract current implementations as "Standard" variants

Rename/refactor current implementations to `StandardHeading`, `StandardChecklist`, `StandardBullet`, `StandardCodeBlock`, `StandardParagraph` - keeping the exact same code but as separate composables.

#### D. Add Obsidianite-specific composables

```kotlin
// --- Obsidianite Heading ---
@Composable
private fun ObsidianiteHeading(block: PreviewBlock.Heading) {
    val (textColor, barColor, topPadding) = when (block.level) {
        1 -> Triple(ObsidianiteTextAccent, ObsidianiteInteractiveAccent, 16.dp)
        2 -> Triple(Color(0xFFCBD5E0), ObsidianiteInteractiveAccent.copy(alpha = 0.3f), 12.dp)
        3 -> Triple(Color(0xFFCBD5E0), ObsidianiteInteractiveAccent.copy(alpha = 0.2f), 10.dp)
        else -> Triple(ObsidianiteTextNormal, ObsidianiteInteractiveAccent.copy(alpha = 0.1f), 8.dp)
    }
    
    val textStyle = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    
    Row(
        modifier = Modifier
            .padding(top = topPadding, bottom = 6.dp)
            .fillMaxWidth()
    ) {
        // Visual left bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(barColor)
                .padding(end = 8.dp)
        )
        
        Text(
            text = block.text,
            style = textStyle.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// --- Obsidianite Checklist ---
@Composable
private fun ObsidianiteChecklist(block: PreviewBlock.Checklist) {
    val textColor = if (block.checked) ObsidianiteTextFaint else ObsidianiteTextNormal
    
    Box(
        modifier = Modifier
            .padding(start = (block.indentLevel * 12).dp, top = 2.dp, bottom = 2.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Custom checkbox appearance
            if (block.checked) {
                Icon(
                    imageVector = Icons.Default.CheckBox,
                    contentDescription = null,
                    tint = ObsidianiteTextAccent,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(20.dp)
                        .padding(top = 2.dp)
                        .border(
                            width = 1.5.dp,
                            color = ObsidianiteTextFaint.copy(alpha = 0.4f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                        )
                )
            }
            
            Text(
                text = buildInlineStyledText(
                    text = block.text,
                    isOrg = true,
                    linkColor = ObsidianiteTextLink,
                    codeBackground = ObsidianiteSurface,
                    codeTextColor = ObsidianiteTextNormal
                ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (block.checked) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = textColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// --- Obsidianite Bullet ---
@Composable
private fun ObsidianiteBullet(block: PreviewBlock.Bullet) {
    Box(
        modifier = Modifier
            .padding(start = (block.indentLevel * 12).dp, top = 2.dp, bottom = 2.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyLarge,
                color = ObsidianiteTextDim,  // Dim blue for bullet
                modifier = Modifier.width(20.dp)
            )
            Text(
                text = buildInlineStyledText(
                    text = block.text,
                    isOrg = true,
                    linkColor = ObsidianiteTextLink,
                    codeBackground = ObsidianiteSurface,
                    codeTextColor = ObsidianiteTextNormal
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = ObsidianiteTextNormal,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// --- Obsidianite Code Block ---
@Composable
private fun ObsidianiteCodeBlock(block: PreviewBlock.CodeBlock) {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .background(ObsidianiteSurface, shape = MaterialTheme.shapes.small)
            .border(
                width = 1.dp,
                color = ObsidianiteBorder,
                shape = MaterialTheme.shapes.small
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Language tag
            if (!block.languageHint.isNullOrBlank()) {
                Text(
                    text = block.languageHint.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = ObsidianiteTextSubAccent.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Text(
                text = block.content.ifBlank { " " },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                ),
                color = ObsidianiteTextNormal
            )
        }
    }
}

// --- Obsidianite Paragraph ---
@Composable
private fun ObsidianiteParagraph(block: PreviewBlock.Paragraph) {
    Text(
        text = buildInlineStyledText(
            text = block.text,
            isOrg = true,
            linkColor = ObsidianiteTextLink,
            codeBackground = ObsidianiteSurface,
            codeTextColor = ObsidianiteTextNormal
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = ObsidianiteTextNormal,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
```

#### E. Update buildInlineStyledText for better inline code styling

The existing function should work, but Obsidianite-specific styling is passed via parameters. Consider enhancing inline code appearance:

```kotlin
// In appendStyledSegment, the code span style could be enhanced:
else -> SpanStyle(
    fontFamily = FontFamily.Monospace,
    background = codeBackground.copy(alpha = 0.8f),
    color = codeTextColor,
    // Add subtle padding effect via additional styling if needed
)
```

---

### 4. EditorScreen.kt
**Location:** `app/src/main/java/com/offlinenotes/ui/editor/EditorScreen.kt`

**Changes:** Pass the current theme palette to `NotePreviewContent`

```kotlin
// Find where NotePreviewContent is called and update:
NotePreviewContent(
    text = text,
    isOrg = isOrg,
    palette = currentPalette,  // Add this parameter - needs to be available in scope
    modifier = Modifier.fillMaxSize()
)
```

You'll need to ensure `currentPalette` is available in EditorScreen. It likely comes from a ViewModel or is passed as a parameter.

---

## Implementation Order

1. **ThemeSettings.kt** - Add enum value (trivial, low risk)
2. **Color.kt** - Add Obsidianite tokens and update offlineColorScheme (low risk)
3. **Test compilation** - Ensure app builds with new palette
4. **NotePreview.kt** - Refactor into standard/Obsidianite variants (medium risk)
   - First extract existing code into `StandardXxx` functions
   - Verify existing behavior unchanged
   - Then add `ObsidianiteXxx` functions
5. **EditorScreen.kt** - Wire up palette parameter (low risk)
6. **Manual testing** - Verify both Org and Markdown previews work

---

## Potential Issues & Mitigations

### Issue 1: EditorScreen doesn't have access to current palette
**Mitigation:** The palette should be available from ThemeViewModel or passed down from MainActivity/OfflineNotesApp. If not, add it to EditorScreen parameters or access via composition local.

### Issue 2: CompositionLocal vs Parameter passing
**Decision:** Use explicit parameter passing for palette to keep it simple and testable. Avoid CompositionLocal unless preview-specific tokens become complex.

### Issue 3: Light mode with Obsidianite
**Mitigation:** Obsidianite is dark-only. Map light mode to dark tokens (as implemented) or create a simplified light variant later if requested.

### Issue 4: Preview performance with conditional styling
**Mitigation:** The `isObsidianite` check is a simple enum comparison, negligible performance impact.

### Issue 5: Inline code styling limitations
**Mitigation:** SpanStyle background works well for inline code. The padding is limited but acceptable for a preview.

---

## Code Summary (Files Changed)

| File | Lines Added | Lines Modified | Risk |
|------|-------------|----------------|------|
| ThemeSettings.kt | 1 | 0 | Low |
| Color.kt | ~50 | 2 | Low |
| NotePreview.kt | ~200 | ~80 | Medium |
| EditorScreen.kt | 0 | 1 | Low |

---

## Testing Checklist

1. **Compilation:** App builds without errors
2. **Settings:** "Obsidianite" appears in palette selection and can be selected
3. **Org Preview - Headings:** 
   - H1 shows cyan accent color with left bar
   - H2-H3 show lighter text with varying left bar opacity
   - Proper spacing before headings
4. **Org Preview - Lists:**
   - Bullets show dim blue color
   - No card background, clean list appearance
   - Indentation works correctly
5. **Org Preview - Checklists:**
   - Checked items show cyan checkbox with strikethrough text
   - Unchecked items show subtle outline checkbox
   - Faint text color for completed items
6. **Org Preview - Code Blocks:**
   - Dark background (#191621)
   - Subtle cyan border
   - Language tag visible in pink/sub-accent color
7. **Org Preview - Inline Styles:**
   - Bold and italic work
   - Inline code has background
   - Links show correct blue color (#6bcafb)
8. **Markdown Preview:** Unchanged behavior when isOrg=false
9. **Other Themes:** TokyoNight, Catppuccin, RosePine still work correctly
10. **Theme Persistence:** Selected Obsidianite persists across app restarts

---

## Notes for Implementer

- Keep changes minimal and focused
- Follow existing code style (Ktlint-compatible)
- Don't over-engineer; the conditional routing in NotePreviewContent is sufficient
- The visual "left bar" for headings is a simple Box with background
- Language tags for code blocks are shown in UPPERCASE with pink/sub-accent color at 80% opacity
- If any issues arise with EditorScreen wiring, check how other screens access the current theme
