package com.example.budsbattery.ui

import android.service.quicksettings.TileService
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budsbattery.R

private val CARD_MARGIN = 12.dp
private val CARD_PADDING = 20.dp
private val CARD_CORNER = 32.dp
private const val CARD_START_SCALE = 0.92f
private const val CARD_DAMPING = 0.8f
private const val CARD_STIFFNESS = 500f

/**
 * Shows a card over the Quick Settings panel, the closest Android lets an app get to an expanding tile.
 *
 * @description Android has no API for a tile that grows in place, so the card is a dialog the tile owns.
 * It sits on top of the open panel instead of closing it; tapping outside or Back dismisses it.
 * @param content - the card's content
 */
fun TileService.showPanelCard(content: @Composable ColumnScope.() -> Unit) {
    val dialog = ComponentDialog(this, R.style.Theme_BudsBattery_PanelCard)
    // ComponentDialog gives the view the lifecycle Compose needs, which a service alone can't
    dialog.setContentView(ComposeView(dialog.context).apply { setContent { BudsTheme { PanelCard(content) } } })
    dialog.setCanceledOnTouchOutside(true)
    dialog.window?.apply {
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setGravity(Gravity.BOTTOM)
    }
    showDialog(dialog)
}

/**
 * Title and status line shared by the panel cards.
 *
 * @param title - card title
 * @param status - one short line under it
 */
@Composable
fun PanelCardHeader(title: String, status: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    // Starts hidden so the card springs up from the edge as it opens
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(spring(CARD_DAMPING, CARD_STIFFNESS)) { it / 2 } +
            scaleIn(spring(CARD_DAMPING, CARD_STIFFNESS), initialScale = CARD_START_SCALE) +
            fadeIn(),
    ) {
        Surface(
            shape = RoundedCornerShape(CARD_CORNER),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(CARD_MARGIN),
        ) {
            Column(modifier = Modifier.padding(CARD_PADDING), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}
