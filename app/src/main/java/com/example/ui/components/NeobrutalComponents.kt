package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun NeobrutalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = NeobrutalWhite,
    borderColor: Color = NeobrutalBlack,
    shadowColor: Color = NeobrutalBlack,
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 4.dp,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .then(tagModifier)
            .padding(end = shadowOffset, bottom = shadowOffset)
    ) {
        // Hard-edged drop shadow box behind matching exact content size
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor)
        )

        // Main Card Foreground
        val clickModifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .border(BorderStroke(borderWidth, borderColor))
                .then(clickModifier)
        ) {
            content()
        }
    }
}

@Composable
fun NeobrutalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    containerColor: Color = NeobrutalYellow,
    contentColor: Color = NeobrutalBlack,
    borderColor: Color = NeobrutalBlack,
    shadowColor: Color = NeobrutalBlack,
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 4.dp,
    enabled: Boolean = true,
    testTag: String? = null,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translation by animateDpAsState(
        targetValue = if (isPressed) shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 60),
        label = "press_translation"
    )

    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .then(tagModifier),
        propagateMinConstraints = true
    ) {
        Box(
            modifier = Modifier
                .padding(end = shadowOffset, bottom = shadowOffset),
            propagateMinConstraints = true
        ) {
            // Shadow background matching exact container dimensions
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(shadowColor)
            )

            // Button Surface with tactile translation and matching parent bounds
            Box(
                modifier = Modifier
                    .offset(x = translation, y = translation)
                    .background(if (enabled) containerColor else Color(0xFFD4D4D4))
                    .border(BorderStroke(borderWidth, borderColor))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        icon()
                        if (text != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    if (text != null) {
                        Text(
                            text = text.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = contentColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NeobrutalSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    activeColor: Color = NeobrutalGreen,
    inactiveColor: Color = Color(0xFFE2E2D9),
    thumbColor: Color = NeobrutalWhite,
    borderColor: Color = NeobrutalBlack,
    testTag: String? = null
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = tween(120),
        label = "switch_thumb"
    )
    val clickModifier = if (onCheckedChange != null) {
        Modifier.clickable { onCheckedChange(!checked) }
    } else Modifier

    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .then(tagModifier)
            .padding(end = 3.dp, bottom = 3.dp)
            .then(clickModifier)
    ) {
        // Hard drop shadow
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(30.dp)
                .offset(x = 3.dp, y = 3.dp)
                .background(NeobrutalBlack)
        )
        // Switch track
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(30.dp)
                .background(if (checked) activeColor else inactiveColor)
                .border(BorderStroke(2.5.dp, borderColor)),
            contentAlignment = Alignment.CenterStart
        ) {
            // Square Brutalist Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(22.dp)
                    .background(thumbColor)
                    .border(BorderStroke(2.dp, borderColor))
            )
        }
    }
}

@Composable
fun NeobrutalRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    activeColor: Color = NeobrutalYellow,
    testTag: String? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .then(tagModifier)
            .padding(end = 2.dp, bottom = 2.dp)
            .then(clickModifier)
    ) {
        // Hard drop shadow
        Box(
            modifier = Modifier
                .size(22.dp)
                .offset(x = 2.dp, y = 2.dp)
                .background(NeobrutalBlack)
        )
        // Outer square
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(NeobrutalWhite)
                .border(BorderStroke(2.5.dp, NeobrutalBlack)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                // Inner solid square
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(NeobrutalBlack)
                )
            }
        }
    }
}

@Composable
fun NeobrutalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeobrutalWhite,
    contentColor: Color = NeobrutalBlack,
    borderColor: Color = NeobrutalBlack,
    shadowOffset: Dp = 4.dp,
    size: Dp = 48.dp,
    testTag: String? = null,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else shadowOffset,
        animationSpec = tween(durationMillis = 80),
        label = "icon_btn_shadow"
    )

    val translation by animateDpAsState(
        targetValue = if (isPressed) shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 80),
        label = "icon_btn_trans"
    )

    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .then(tagModifier)
            .size(size + shadowOffset)
            .padding(end = shadowOffset, bottom = shadowOffset)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(x = currentOffset, y = currentOffset)
                .background(NeobrutalBlack)
        )
        Box(
            modifier = Modifier
                .size(size)
                .offset(x = translation, y = translation)
                .background(containerColor)
                .border(BorderStroke(3.dp, borderColor))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
            }
        }
    }
}

@Composable
fun NeobrutalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    testTag: String = "neobrutal_text_field",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .padding(end = 4.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(NeobrutalBlack)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeobrutalWhite)
                .border(BorderStroke(3.dp, NeobrutalBlack))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = NeobrutalBlack
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(NeobrutalBlack),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions
                )
            }
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(6.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun NeobrutalBadge(
    text: String,
    color: Color = NeobrutalYellow,
    textColor: Color = NeobrutalBlack,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(end = 2.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(NeobrutalBlack)
        )
        Box(
            modifier = Modifier
                .background(color)
                .border(BorderStroke(2.dp, NeobrutalBlack))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = text.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = textColor
                )
            )
        }
    }
}

@Composable
fun NeobrutalAppBar(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeobrutalYellow,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .border(BorderStroke(0.dp, Color.Transparent))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = NeobrutalBlack,
                    letterSpacing = 0.5.sp
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
        // Heavy 4.dp bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.BottomCenter)
                .background(NeobrutalBlack)
        )
    }
}

@Composable
fun NeobrutalDialog(
    onDismissRequest: () -> Unit,
    title: String,
    titleColor: Color = NeobrutalYellow,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 6.dp, bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(NeobrutalBlack)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeobrutalWhite)
                    .border(BorderStroke(3.dp, NeobrutalBlack))
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(titleColor)
                        .border(BorderStroke(2.dp, NeobrutalBlack))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeobrutalBlack
                        )
                    )
                    Text(
                        text = "[X]",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeobrutalBlack
                        ),
                        modifier = Modifier.clickable { onDismissRequest() }
                    )
                }

                // Content
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}
