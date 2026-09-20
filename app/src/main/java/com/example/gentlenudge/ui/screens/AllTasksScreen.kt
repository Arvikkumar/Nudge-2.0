package com.example.gentlenudge.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.deepdive.DeepDiveManager
import com.example.gentlenudge.ui.components.DeepDiveCard
import com.example.gentlenudge.ui.components.TaskSlipItem
import com.example.gentlenudge.ui.theme.NudgeBlue
import com.example.gentlenudge.ui.theme.NudgeBlueContainer

@Composable
fun AllTasksScreen(
    tasks: List<NudgeTask>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleDone: (NudgeTask) -> Unit,
    onSnooze: (NudgeTask, String) -> Unit,
    onDelete: (NudgeTask) -> Unit,
    onEdit: (NudgeTask) -> Unit,
    onOpenComposer: () -> Unit,
    onDeleteMultiple: (List<NudgeTask>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTasks = remember(tasks) { tasks.filter { !it.isDone } }
    val completedTasks = remember(tasks) { tasks.filter { it.isDone } }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedTaskIds = remember { mutableStateListOf<Long>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // If there are no completed tasks left, exit selection mode cleanly
    LaunchedEffect(completedTasks.isEmpty()) {
        if (completedTasks.isEmpty() && isSelectionMode) {
            isSelectionMode = false
            selectedTaskIds.clear()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("all_tasks_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Editorial Page Heading
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Your notes,",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 34.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "always kept close.",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 34.sp
                    ),
                    color = NudgeBlue
                )
            }
        }

        // Search box
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_tasks_input"),
                placeholder = {
                    Text(
                        "Search, find, and keep moving.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NudgeBlueContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${tasks.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = NudgeBlue
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NudgeBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
        }

        // Reusable Deep Dive quick-action card (permanent, separate from regular tasks)
        item {
            val deepDiveContext = androidx.compose.ui.platform.LocalContext.current
            val deepDiveState by DeepDiveManager.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                DeepDiveManager.init(deepDiveContext)
            }

            DeepDiveCard(
                state = deepDiveState,
                onStartSession = { endTimeMillis, style ->
                    DeepDiveManager.startSession(deepDiveContext, endTimeMillis, style)
                },
                onEndSession = {
                    DeepDiveManager.endSession(deepDiveContext)
                }
            )
        }

        // Tasks list or Empty state
        if (tasks.isEmpty()) {
            item {
                EmptyNudgeCard(
                    message = if (searchQuery.isNotBlank()) "No nudges found for \"$searchQuery\"." else "No notes yet.",
                    onAdd = onOpenComposer
                )
            }
        } else {
            // ACTIVE SECTION
            if (activeTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NudgeBlueContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${activeTasks.size} to do",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = NudgeBlue
                            )
                        }
                    }
                }

                items(activeTasks, key = { it.id }) { task ->
                    TaskSlipItem(
                        task = task,
                        onToggleDone = { onToggleDone(task) },
                        onSnooze = { label -> onSnooze(task, label) },
                        onDelete = { onDelete(task) },
                        onEdit = { onEdit(task) }
                    )
                }
            }

            // COMPLETED SECTION
            if (completedTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (activeTasks.isNotEmpty()) 10.dp else 6.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPLETED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${completedTasks.size} done",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Tiny subtle selection icon beside the done badge
                            IconButton(
                                onClick = {
                                    isSelectionMode = !isSelectionMode
                                    if (!isSelectionMode) {
                                        selectedTaskIds.clear()
                                    }
                                },
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("completed_selection_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = "Bulk select completed nudges",
                                    tint = if (isSelectionMode) NudgeBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Selection toolbar when selection mode is active
                if (isSelectionMode) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .testTag("completed_selection_toolbar"),
                            shape = RoundedCornerShape(12.dp),
                            color = NudgeBlueContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, NudgeBlue.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            isSelectionMode = false
                                            selectedTaskIds.clear()
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("cancel_selection_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel selection",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "${selectedTaskIds.size} selected",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (selectedTaskIds.isNotEmpty()) {
                                        TextButton(
                                            onClick = { showDeleteConfirmation = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("delete_selected_button")
                                        ) {
                                            Text(
                                                text = "Delete (${selectedTaskIds.size})",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 12.5.sp
                                                )
                                            )
                                        }
                                    }

                                    val isAllSelected = completedTasks.isNotEmpty() && completedTasks.all { it.id in selectedTaskIds }
                                    TextButton(
                                        onClick = {
                                            if (isAllSelected) {
                                                selectedTaskIds.clear()
                                            } else {
                                                selectedTaskIds.clear()
                                                selectedTaskIds.addAll(completedTasks.map { it.id })
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("select_all_button")
                                    ) {
                                        Text(
                                            text = if (isAllSelected) "Deselect all" else "Select all",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = NudgeBlue,
                                                fontSize = 12.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(completedTasks, key = { it.id }) { task ->
                    val isSelected = task.id in selectedTaskIds
                    TaskSlipItem(
                        task = task,
                        onToggleDone = { onToggleDone(task) },
                        onSnooze = { label -> onSnooze(task, label) },
                        onDelete = { onDelete(task) },
                        onEdit = { onEdit(task) },
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectToggle = {
                            if (isSelected) {
                                selectedTaskIds.remove(task.id)
                            } else {
                                selectedTaskIds.add(task.id)
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showDeleteConfirmation && selectedTaskIds.isNotEmpty()) {
        val count = selectedTaskIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = if (count == 1) "Delete 1 completed nudge?" else "Delete $count completed nudges?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (count == 1) "This completed nudge will be permanently removed." else "These $count completed nudges will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tasksToDelete = completedTasks.filter { it.id in selectedTaskIds }
                        onDeleteMultiple(tasksToDelete)
                        selectedTaskIds.clear()
                        isSelectionMode = false
                        showDeleteConfirmation = false
                    },
                    modifier = Modifier.testTag("confirm_bulk_delete_button")
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.testTag("cancel_bulk_delete_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
