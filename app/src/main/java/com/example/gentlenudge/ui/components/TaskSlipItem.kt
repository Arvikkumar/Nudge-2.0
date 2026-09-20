package com.example.gentlenudge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ripple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TaskAttachment
import com.example.gentlenudge.ui.theme.ImportantDot
import com.example.gentlenudge.ui.theme.NudgeBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskSlipItem(
    task: NudgeTask,
    onToggleDone: () -> Unit,
    onSnooze: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    compact: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val checkboxInteractionSource = remember { MutableInteractionSource() }
    val contentInteractionSource = remember { MutableInteractionSource() }

    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(task.id, task.repeat, task.timeLabel) {
        if (RecurringReminderUiHelper.isRecurringReminder(task.repeat)) {
            while (true) {
                delay(30_000L)
                currentTimeMillis = System.currentTimeMillis()
            }
        }
    }

    val context = LocalContext.current
    val recurringDisplay = remember(task, currentTimeMillis) {
        RecurringReminderUiHelper.getRecurringReminderDisplay(
            task = task,
            context = context,
            now = java.time.LocalDateTime.now(),
            nowMillis = currentTimeMillis
        )
    }

    val accentColor = NudgeBlue

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectionMode && isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else if (task.isDone) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelectionMode && isSelected) {
            BorderStroke(1.5.dp, NudgeBlue)
        } else {
            CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(MaterialTheme.colorScheme.outline),
                width = 1.dp
            )
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Left vertical colored accent stripe (as seen in screenshots)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 3.dp)
                    .width(4.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isSelectionMode && isSelected) {
                            accentColor
                        } else if (task.isDone) {
                            accentColor.copy(alpha = 0.35f)
                        } else {
                            accentColor
                        }
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox / Selection Control
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = checkboxInteractionSource,
                            indication = ripple(
                                bounded = true,
                                radius = 20.dp,
                                color = NudgeBlue.copy(alpha = 0.2f)
                            ),
                            onClick = {
                                if (isSelectionMode && onSelectToggle != null) {
                                    onSelectToggle()
                                } else {
                                    onToggleDone()
                                }
                            }
                        )
                        .testTag(if (isSelectionMode) "task_select_${task.id}" else "task_check_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelectionMode) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(NudgeBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.5.dp,
                                        color = NudgeBlue.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    } else if (task.isDone) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NudgeBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Task Content (Clickable to edit or toggle select)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = contentInteractionSource,
                            indication = if (isSelectionMode || onEdit != null) ripple(bounded = true, color = NudgeBlue.copy(alpha = 0.1f)) else null,
                            enabled = isSelectionMode || onEdit != null
                        ) {
                            if (isSelectionMode && onSelectToggle != null) {
                                onSelectToggle()
                            } else {
                                onEdit?.invoke()
                            }
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (task.isDone) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.SemiBold,
                                fontSize = 15.5.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (task.priority.equals("Important", ignoreCase = true) && !task.isDone) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(ImportantDot)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Meta Row: Time icon + Time label + Category Tag
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Time info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = recurringDisplay?.displayString ?: task.timeLabel.ifBlank { "Any time" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Repeat note
                        if (task.repeat != "Does not repeat") {
                            Text(
                                text = "• ${task.repeat}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Attachments indicator
                        if (task.attachments.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                val hasAudio = task.attachments.any { it.type == TaskAttachment.TYPE_AUDIO }
                                val hasImage = task.attachments.any { it.type == TaskAttachment.TYPE_IMAGE }
                                val icon = when {
                                    hasAudio -> Icons.Outlined.Mic
                                    hasImage -> Icons.Outlined.Photo
                                    else -> Icons.Outlined.AttachFile
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Attachments",
                                    tint = NudgeBlue,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${task.attachments.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NudgeBlue
                                    )
                                )
                            }
                        }
                    }
                }

                // Three-dots menu (hidden during selection mode)
                if (!isSelectionMode) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("task_menu_${task.id}")
                        ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Task options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (!task.isDone) {
                            Text(
                                text = "Remind me…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("In 30 minutes", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSnooze("in 30 minutes")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tonight", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.NightsStay, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSnooze("tonight")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tomorrow", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSnooze("tomorrow")
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }

                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("Edit nudge", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete nudge",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
}

