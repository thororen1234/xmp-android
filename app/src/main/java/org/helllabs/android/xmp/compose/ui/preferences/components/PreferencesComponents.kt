package org.helllabs.android.xmp.compose.ui.preferences.components

import androidx.annotation.IntRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.storage.base.getValue
import com.alorma.compose.settings.storage.base.rememberFloatSettingState
import com.alorma.compose.settings.storage.base.setValue

@Composable
internal fun AboutText(string: String, textAlign: TextAlign = TextAlign.Center) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        text = string,
        textAlign = textAlign
    )
}

/**
 * Modified from com.alorma.compose.settings.ui.SettingsSlider
 * Modifications:
 *      Subtitle Text with Slider
 */
@Composable
internal fun XmpSettingsSlider(
    modifier: Modifier = Modifier,
    state: SettingValueState<Float> = rememberFloatSettingState(),
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    onValueChange: (Float) -> Unit = {},
    sliderModifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors()
) {
    var settingValue by state
    Surface {
        Row(
            modifier = modifier
                .height(96.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            XmpSettingsTileSlider(
                title = title,
                subtitle = subtitle,
                value = settingValue,
                onValueChange = { value ->
                    settingValue = value
                    onValueChange(settingValue)
                },
                modifier = sliderModifier,
                icon = icon,
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished,
                interactionSource = interactionSource,
                colors = colors
            )
        }
    }
}

@Composable
internal fun XmpSettingsTileSlider(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit?)? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors()
) {
    XmpSettingsTileScaffold(
        enabled = enabled,
        title = title,
        subtitle = {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp, end = 0.dp)
                    .then(modifier)
            ) {
                subtitle?.let {
                    it()
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.then(modifier),
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                    interactionSource = interactionSource,
                    colors = colors
                )
            }
        },
        icon = icon
    )
}

@Composable
internal fun XmpSettingsTileScaffold(
    enabled: Boolean = true,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    action: (@Composable (Boolean) -> Unit)? = null,
    actionDivider: Boolean = false
) {
    val minHeight = if (subtitle == null) 72.dp else 88.dp
    ListItem(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .defaultMinSize(minHeight = minHeight),
        headlineContent = {
            XmpWrapContentColor(enabled = enabled) {
                title()
            }
        },
        supportingContent = if (subtitle == null) {
            null
        } else {
            {
                XmpWrapContentColor(enabled = enabled) {
                    subtitle()
                }
            }
        },
        leadingContent = if (icon == null) {
            null
        } else {
            {
                XmpWrapContentColor(enabled = enabled) {
                    icon()
                }
            }
        },
        trailingContent = if (action == null) {
            null
        } else {
            {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (actionDivider) {
                        val color = DividerDefaults.color.copy(
                            alpha = if (enabled) {
                                1f
                            } else {
                                0.6f
                            }
                        )
                        Divider(
                            color = color,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxHeight()
                                .width(1.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    action(enabled)
                }
            }
        }
    )
}

@Composable
internal fun XmpWrapContentColor(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val alpha = if (enabled) 1.0f else 0.6f
    val contentColor = LocalContentColor.current.copy(alpha = alpha)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        content()
    }
}
