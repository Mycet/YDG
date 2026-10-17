package dev.mycet.ydg.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.ui.theme.AppTheme
import dev.mycet.ydg.ui.theme.Sizes
import dev.mycet.ydg.utils.fieldPadding
import dev.mycet.ydg.utils.formatPathMiddle
import dev.mycet.ydg.utils.toSVG

data class BevelButtonColors(
    val border: Color,
    val borderHover: Color,
    val fill: Color,
    val fillHover: Color,
    val fontColor: Color,
    val fontColorHover: Color
)
data class BevelButtonFont(
    val fontSize: TextUnit,
    val fontWeight: FontWeight
)

object BevelButtonDefaults {
    @Composable
    fun colors(
        border: Color = AppTheme.Border,
        borderHover: Color = if (border == AppTheme.Accent) AppTheme.AccentHover else AppTheme.BorderHover,
        fill: Color = AppTheme.SurfaceHigh, fillHover: Color = AppTheme.SurfaceHigh,
        textColor: Color = AppTheme.Font1, textColorHover: Color = AppTheme.Font1
    ) = BevelButtonColors(
        border = border, borderHover = borderHover,
        fill = fill, fillHover = fillHover,
        fontColor = textColor, fontColorHover = textColorHover
    )

    @Composable
    fun font(
        fontSize: TextUnit = Sizes.FieldFontSize,
        fontWeight: FontWeight = FontWeight.Normal
    ) = BevelButtonFont(
        fontSize = fontSize, fontWeight = fontWeight
    )
}

// 2. Componente base (Cascarón biselado que recibe cualquier contenido)
@Composable
fun BevelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 26.dp,
    colors: BevelButtonColors = BevelButtonDefaults.colors(),
    content: @Composable RowScope.(isHovered: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val currentBorder by animateColorAsState(if (isHovered) colors.borderHover else colors.border)
    val currentFill by animateColorAsState(if (isHovered) colors.fillHover else colors.fill)

    val widthModifier = if (width != null) Modifier.width(width) else Modifier.wrapContentWidth()
    val innerWidthModifier = if (width != null) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()

    Box(
        modifier = modifier
            .then(widthModifier)
            .height(height)
            .background(currentBorder, Sizes.Round8)
            .padding(1.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(innerWidthModifier)
                .fillMaxHeight()
                .background(currentFill, Sizes.Round8)
                .clickable(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                content(isHovered)
            }
        }
    }
}

@Composable
fun BevelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null, // Ruta al archivo SVG (ej: "icons/download.svg")
    iconSize: Dp = 14.dp,
    width: Dp? = null,
    height: Dp = 26.dp,
    colors: BevelButtonColors = BevelButtonDefaults.colors(),
    font: BevelButtonFont = BevelButtonDefaults.font(),
) {
    BevelButton(
        onClick = onClick,
        modifier = modifier,
        width = width,
        height = height,
        colors = colors
    ) { isHovered ->
        val currentFont by animateColorAsState(if (isHovered) colors.fontColorHover else colors.fontColor)

        if (icon != null) {
            var path = if (icon.startsWith("icons/")) icon else "icons/$icon"
            if (!path.endsWith(".svg")) path = "$path.svg"
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(path),
                    contentDescription = null,
                    tint = currentFont,
                    modifier = Modifier.size(iconSize).offset(y = (-0.5).dp)
                )
            }
            if (text.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = currentFont,
                fontSize = font.fontSize,
                fontWeight = font.fontWeight,
                modifier = Modifier.offset(y = (-1.5).dp)
            )
        }
    }
}

@Composable
fun BevelContainer(
    modifier: Modifier = Modifier,
    border: Color = AppTheme.Border,
    border2: Color = AppTheme.Background,
    fill: Color = AppTheme.Sunken,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = Sizes.Round8

    Box(
        modifier = modifier
            .background(border, shape)
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(border2, shape)
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fill, shape),
                contentAlignment = Alignment.CenterStart,
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable // Dropdown genérico
fun <T> FieldDropdown(
    value: AnnotatedString,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<T>,
    onSelect: (T) -> Unit,
    label: (T) -> AnnotatedString,
    modifier: Modifier = Modifier.width(200.dp),
    height: Dp = Sizes.FieldHeightText
) {
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { isOpening ->
            if (isOpening)
                focusManager.clearFocus()

            onExpandedChange(isOpening)
        },
        modifier = modifier.height(height)
    ) {
        BevelContainer(
            border = if (expanded) AppTheme.Accent else AppTheme.Border,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .height(height),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
            ) {
                Text(text = value, color = AppTheme.Font1, fontSize = Sizes.FieldFontSize, modifier = Modifier.weight(1f))

                CompositionLocalProvider(LocalContentColor provides if (expanded) AppTheme.Accent else AppTheme.Border) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = AppTheme.Sunken,
            shape = Sizes.Round8
        ) {
            options.forEach { option ->
                val isSelected = label(option).text == value.text

                DropdownMenuItem(
                    text = {
                        Text(
                            label(option),
                            color = AppTheme.Font1,
                            fontSize = Sizes.FieldFontSize
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                painter = painterResource("check".toSVG()),
                                contentDescription = "Selected",
                                modifier = Modifier.size(14.dp),
                                tint = AppTheme.Accent
                            )
                        }
                    },
                    modifier = Modifier.height(Sizes.FieldHeightText),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FieldDropdown(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<T>,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier.width(Sizes.FieldWidthDropdown),
    height: Dp = Sizes.FieldHeightText
) = FieldDropdown(
    value = AnnotatedString(value),
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    options = options,
    onSelect = onSelect,
    label = { AnnotatedString(label(it)) },
    modifier = modifier,
    height = height
)

@Composable
fun FieldText(
    modifier: Modifier = Modifier.width(Sizes.FieldWidthText).height(Sizes.FieldHeightText),
    value: String,
    onValueChange: (String) -> Unit,
    color: Color = AppTheme.Font1,
    maxLength: Int? = null,
    outerContent: (@Composable () -> Unit)? = null,
    innerContent: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    BevelContainer(
        border = if (isFocused) AppTheme.Accent else AppTheme.Border,
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = color,
            ),
            cursorBrush = SolidColor(color),
            singleLine = true,
            modifier = Modifier.fillMaxSize()
                .onFocusChanged { isFocused = it.isFocused},
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().fieldPadding()
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerContent()
                        if (!isFocused && value.isNotEmpty()) {
                            Text(
                                text = if (maxLength == null) value else value.formatPathMiddle(maxLength),
                                color = color,
                                fontSize = Sizes.FieldFontSize,
                                maxLines = 1
                            )
                        } else
                            innerTextField()
                    }

                    if (outerContent != null)
                        outerContent()
                }
            }
        )
    }
}

@Composable
fun FieldSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 25.5.dp else 4.dp,
        animationSpec = tween(durationMillis = 150)
    )
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(26.dp)
            .background(AppTheme.Sunken, Sizes.Round12)
            .border(1.dp, AppTheme.Border, Sizes.Round12)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(18.dp)
                .background(
                    if (checked) AppTheme.Accent else AppTheme.Font2,
                    CircleShape
                )
        )
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .background(AppTheme.Background)
                .padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(if (expanded) "icons/arrow-down.svg" else "icons/arrow-right.svg"),
                contentDescription = "expand",
                tint = AppTheme.Font1,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = AppTheme.Font1,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // AnimatedVisibility permite que sea visible solo cuando `expanded = true`
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content() // el contenido que se pase por argumento, los botones que tenga la sección
            }
        }
    }
}