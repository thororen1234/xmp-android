package org.helllabs.android.xmp.compose.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

private val pattern = Regex("http://|https://")

/**
 * Creates a string where the whole text string is clickable
 */
@Composable
fun annotatedLinkStringCombined(
    text: String,
    url: String
): AnnotatedString = buildAnnotatedString {
    addStyle(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        ),
        start = 0,
        end = text.length
    )
    append(text)
    addStringAnnotation(
        tag = "URL",
        annotation = url,
        start = 0,
        end = text.length
    )
}

/**
 * Creates a sting where the URL is shown and clickable
 */
@Composable
fun annotatedLinkString(
    text: String,
    url: String
): AnnotatedString = buildAnnotatedString {
    val string = "$text $url"
    append(string)
    addStyle(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        ),
        start = string.length - url.length,
        end = string.length
    )
    addStringAnnotation(
        tag = "URL",
        annotation = if (url.lowercase().contains(pattern)) {
            url
        } else {
            "https://$url"
        },
        start = string.length - url.length,
        end = string.length
    )
}
