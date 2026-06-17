package com.iranconnection.app.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrowserAddressBar(
    url: String,
    tabCount: Int,
    isLoading: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    progress: Int,
    onLoadUrl: (String) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onShowTabs: () -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var textFieldValue by remember { mutableStateOf(url) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (!isFocused) textFieldValue = url
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(enabled = canGoBack) { onGoBack() },
                contentAlignment = Alignment.Center,
            ) {
                BackArrowIcon(
                    color = if (canGoBack) Color(0xFF1A1A1A) else Color(0xFFCCCCCC),
                )
            }

            // Forward button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(enabled = canGoForward) { onGoForward() },
                contentAlignment = Alignment.Center,
            ) {
                ForwardArrowIcon(
                    color = if (canGoForward) Color(0xFF1A1A1A) else Color(0xFFCCCCCC),
                )
            }

            // Address field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(Color(0xFFF1F3F4), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (!isFocused && textFieldValue.isEmpty()) {
                    Text(
                        "Search or enter address",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = Color(0xFF1A1A1A),
                        fontFamily = FontFamily.Default,
                    ),
                    cursorBrush = SolidColor(Color(0xFF1A73E8)),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Uri,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { onLoadUrl(textFieldValue) },
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                )
            }

            // Refresh / Stop button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { if (isLoading) onStop() else onRefresh() },
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CloseIcon(color = Color(0xFF1A1A1A))
                } else {
                    RefreshIcon(color = Color(0xFF1A1A1A))
                }
            }

            // Tab count button (Chrome-style square badge)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onShowTabs() }
                    .border(1.5.dp, Color(0xFF1A1A1A), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (tabCount > 99) ":D" else tabCount.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                )
            }
        }

        // Progress bar + divider sit under the row (bar is at top of screen).
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFF1A73E8),
                trackColor = Color.Transparent,
            )
        } else {
            Box(modifier = Modifier.height(2.dp))
        }
        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
    }
}
