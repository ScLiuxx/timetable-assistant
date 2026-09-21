package com.kyant.backdrop.catalog.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop> {
    error("No glass backdrop provided")
}

/** 当前应用主题强调色，可在根节点通过 [LocalAccentColor] 提供。 */
val LocalAccentColor = staticCompositionLocalOf<Color> {
    Color(0xFF0A84FF)
}

@Composable
fun isLightTheme(): Boolean = !isSystemInDarkTheme()

@Composable
fun contentColor(): Color = if (isLightTheme()) Color(0xFF111114) else Color(0xFFF4F4F6)

@Composable
fun secondaryContentColor(): Color =
    if (isLightTheme()) Color(0xFF111114).copy(alpha = 0.55f)
    else Color(0xFFF4F4F6).copy(alpha = 0.6f)

@Composable
fun accentColor(): Color = LocalAccentColor.current

@Composable
fun glassSurfaceColor(): Color =
    if (isLightTheme()) Color(0xFFFFFFFF).copy(alpha = 0.38f)
    else Color(0xFF16161A).copy(alpha = 0.42f)

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color = Color.Unspecified,
    tintAlpha: Float = 0.28f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = LocalGlassBackdrop.current
    val surface = glassSurfaceColor()
    val shadowColor = Color.Black.copy(alpha = if (isLightTheme()) 0.12f else 0.35f)
    val interactionSource = if (onClick != null) {
        remember { MutableInteractionSource() }
    } else {
        null
    }
    Box(
        modifier
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(6f.dp.toPx())
                },
                highlight = { Highlight.Default },
                shadow = if (shadow) {
                    { Shadow(radius = 10f.dp, color = shadowColor) }
                } else {
                    null
                },
                onDrawSurface = {
                    drawRect(surface)
                    if (tint.isSpecified) {
                        drawRect(tint.copy(alpha = tintAlpha))
                    }
                }
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = contentPadding,
        shadow = false,
        content = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    )
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    textStyle: TextStyle? = null
) {
    val color = contentColor()
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        shadow = false
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                BasicText(
                    placeholder,
                    style = textStyle ?: TextStyle(secondaryContentColor(), 15.sp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = textStyle ?: TextStyle(color, 15.sp),
                cursorBrush = SolidColor(accentColor()),
                singleLine = singleLine,
                minLines = minLines
            )
        }
    }
}

@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(20.dp),
                content = {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content
                    )
                }
            )
        }
    }
}

@Composable
fun GlassTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
    color: Color = contentColor()
) {
    BasicText(
        text,
        modifier,
        style = TextStyle(color, fontSize.sp, FontWeight.SemiBold)
    )
}

@Composable
fun GlassLabel(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 14,
    color: Color = secondaryContentColor()
) {
    BasicText(text, modifier, style = TextStyle(color, fontSize.sp))
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        style = TextStyle(secondaryContentColor(), 13.sp, FontWeight.Medium)
    )
}

@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    contentColorOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
) {
    GlassSurface(
        modifier = modifier,
        shape = CircleShape,
        tint = tint,
        contentPadding = contentPadding,
        onClick = onClick,
        shadow = false
    ) {
        BasicText(
            text,
            style = TextStyle(contentColorOverride ?: contentColor(), 14.sp, FontWeight.Medium),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) accentColor() else Color.Unspecified
    GlassSurface(
        modifier = modifier,
        shape = CircleShape,
        tint = tint,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
        onClick = onClick,
        shadow = false
    ) {
        BasicText(
            text,
            style = TextStyle(
                if (selected) Color.White else contentColor(),
                13.sp,
                if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun RowScope.FlexSpacer(weight: Float = 1f) {
    Box(Modifier.weight(weight))
}

@Composable
fun CenteredLabel(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier,
        style = TextStyle(secondaryContentColor(), 14.sp, textAlign = TextAlign.Center)
    )
}
