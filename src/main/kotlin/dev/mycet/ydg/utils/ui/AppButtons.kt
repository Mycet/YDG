package dev.mycet.ydg.utils.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mycet.ydg.utils.AppTheme
import dev.mycet.ydg.utils.Sizes

@Composable
fun BevelButton(
    text: String,
    icon: ImageVector? = null, // null por defecto
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 26.dp,
    fontSize: TextUnit = 13.sp,
    textColor: Color = AppTheme.TextPrimary,
    dark: Color = AppTheme.Accent,
    light: Color = AppTheme.Background,
    fill: Color = AppTheme.Contrast,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(height)
            .wrapContentWidth()
            .background(dark, RoundedCornerShape(8.dp))
            .padding(1.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .background(light, RoundedCornerShape(8.dp))
                .padding(1.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .background(fill, RoundedCornerShape(8.dp))
                    .clickable(
                        onClick = onClick,
                        interactionSource = interactionSource,
                        indication = null
                    )
                    .padding(horizontal = if (icon != null) 6.dp else 12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                } else
                    Text(text, color = textColor, fontSize = fontSize)
            }
        }
    }
}

@Composable
fun BevelContainer(
    modifier: Modifier = Modifier,
    dark: Color = AppTheme.Accent,
    light: Color = AppTheme.Background,
    fill: Color = AppTheme.Contrast,
    cornerRadius: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .background(dark, shape)
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(light, shape)
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
fun <T> SimpleDropdown(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<T>,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier.width(200.dp),
    height: Dp = Sizes.TextField
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.height(height)
    ) {
        BevelContainer(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
                .height(height),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
            ) {
                Text(text = value, color = AppTheme.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))

                CompositionLocalProvider(LocalContentColor provides AppTheme.Accent) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = AppTheme.Contrast
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            label(option),
                            color = AppTheme.TextPrimary,
                            fontSize = Sizes.Font
                        )
                    },
                    modifier = Modifier.height(Sizes.TextField),
                    onClick = { onSelect(option) }
                )
            }
        }
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
                tint = AppTheme.TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = AppTheme.TextPrimary,
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