package org.helllabs.android.xmp.preferences.about

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.theme.themedText
import timber.log.Timber

class About : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            XmpTheme {
                val scrollState = rememberScrollState()
                val isScrolled = remember {
                    derivedStateOf {
                        scrollState.value > 0
                    }
                }

                Scaffold(
                    topBar = {
                        XmpTopBar(
                            title = stringResource(id = R.string.pref_about_title),
                            isScrolled = isScrolled.value,
                            onBack = {
                                onBackPressedDispatcher.onBackPressed()
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .padding(
                                PaddingValues(
                                    top = paddingValues.calculateTopPadding(),
                                    bottom = paddingValues.calculateBottomPadding(),
                                    start = 16.dp,
                                    end = 16.dp
                                )
                            )
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = themedText(text = stringResource(id = R.string.app_name)),
                            textAlign = TextAlign.Center,
                            fontFamily = michromaFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(baselineShift = BaselineShift(.3f))
                        )
                        AboutText(
                            stringResource(
                                id = R.string.about_version,
                                BuildConfig.VERSION_NAME
                            )
                        )
                        AboutText(stringResource(id = R.string.about_author))
                        AboutText(stringResource(id = R.string.about_xmp, Xmp.getVersion()))
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(
                            modifier = Modifier.fillMaxWidth(.85f),
                            color = MaterialTheme.colorScheme.inverseSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            text = stringResource(id = R.string.changelog),
                            fontFamily = michromaFontFamily,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(baselineShift = BaselineShift(.3f))
                        )
                        AboutText(stringResource(id = R.string.changelog_text), TextAlign.Start)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutText(string: String, textAlign: TextAlign = TextAlign.Center) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        text = string,
        textAlign = textAlign
    )
}
