@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)


package com.oss.euphoriae.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

@Composable
fun SimpleTopAppBar(
    title: @Composable (() -> Unit),
    actionIcon: @Composable (() -> Unit),
    onActionClick: () -> Unit
) {
    TopAppBar(
        title = title,
        actions = {
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Start
                    ),
                tooltip = { PlainTooltip { Text("Settings") } },
                state = rememberTooltipState(),
            ) {
                IconButton(
                    onClick = onActionClick,
                ) {
                    actionIcon()
                }
            }
        },
    )
}