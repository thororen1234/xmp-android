package org.helllabs.android.xmp.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.seed

/**
 * Accent the "Xmp" part of the text.
 */
@Composable
fun themedText(text: String): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = seed)) {
            append(text.substring(0, 3))
        }

        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
            append(text.substring(3, text.length))
        }
    }
}

/**
 * Creates a string where the whole text string is clickable.
 */
@Composable
fun annotatedLinkStringCombined(
    text: String,
    url: String
): AnnotatedString {
    val uriHandler = LocalUriHandler.current
    return buildAnnotatedString {
        withLink(
            link = LinkAnnotation.Clickable(
                tag = url,
                linkInteractionListener = { uriHandler.openUri(url) },
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ),
            block = { append(text) }
        )
    }
}

/**
 * Creates a sting where the URL is shown and clickable
 */
@Composable
fun annotatedLinkString(
    text: String,
    url: String
): AnnotatedString {
    val uriHandler = LocalUriHandler.current
    return buildAnnotatedString {
        append(text)
        withLink(
            link = LinkAnnotation.Clickable(
                tag = url,
                linkInteractionListener = { uriHandler.openUri("https://$url") },
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ),
            block = { append(url) }
        )
    }
}

@Preview
@Composable
private fun Preview_Utils() {
    XmpTheme(useDarkTheme = true) {
        Surface {
            Column {
                Text(
                    text = annotatedLinkStringCombined(
                        text = "Link String",
                        url = "https://developer.android.com/"
                    )
                )
                Text(
                    text = annotatedLinkString(
                        text = stringResource(id = R.string.search_provided_by),
                        url = "modarchive.org"
                    )
                )
            }
        }
    }
}
