package com.kyant.backdrop.catalog.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop> {
    error("No glass backdrop provided")
}

/** 当前应用主题强调色，可在根节点通过 [LocalAccentColor] 提供。 */
val LocalAccentColor = staticCompositionLocalOf<Color> {
    Color(0xFF0A84FF)
}

/** 玻璃表面不透明度（0.2 ~ 0.8），越高玻璃越"实"、底层越透不过。 */
val LocalGlassOpacity = staticCompositionLocalOf<Float> { 0.42f }

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
fun glassSurfaceColor(): Color {
    val alpha = LocalGlassOpacity.current
    return if (isLightTheme()) Color(0xFFFFFFFF).copy(alpha = alpha)
    else Color(0xFF16161A).copy(alpha = alpha)
}

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
                    lens(8f.dp.toPx(), 20f.dp.toPx())
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
    val backdrop = LocalGlassBackdrop.current
    val surface = glassSurfaceColor()
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(14.dp) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                onDrawSurface = { drawRect(surface) }
            )
            .then(
                if (focused) {
                    Modifier.border(1.5.dp, accentColor(), RoundedCornerShape(14.dp))
                } else {
                    Modifier.border(0.5.dp, contentColor().copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                }
            )
            .padding(PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty() && !focused) {
                BasicText(
                    placeholder,
                    style = textStyle ?: TextStyle(secondaryContentColor(), 15.sp)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
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
    val backdrop = LocalGlassBackdrop.current
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                layerBlock = {
                    val width = size.width
                    val height = size.height
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
                    val maxOffset = size.minDimension
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
                    val maxDragScale = 4f.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f)
                    scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f)
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text,
            style = TextStyle(contentColorOverride ?: contentColor(), 14.sp, FontWeight.Medium)
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


