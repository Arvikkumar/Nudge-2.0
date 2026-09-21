package com.example.gentlenudge.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.deepdive.DeepDiveManager
import com.example.gentlenudge.ui.components.DeepDiveCard
import com.example.gentlenudge.ui.components.TaskSlipItem
import com.example.gentlenudge.ui.theme.NudgeBlue
import kotlin.math.roundToInt

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
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

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

    val deepDiveState by DeepDiveManager.state.collectAsStateWithLifecycle()
    val positionState by DeepDiveManager.positionState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        DeepDiveManager.init(context)
    }

    var isMoveMode by remember { mutableStateOf(false) }
    var floatingOffset by remember { mutableStateOf<Offset?>(null) }
    var rootLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var inlineSlotCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var cardMeasuredSize by remember { mutableStateOf(IntSize.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var inlineCardHeightDp by remember { mutableStateOf(96.dp) }

    val isFloating = positionState.hasCustomPosition || isMoveMode

    val floatingWidthDp = minOf(configuration.screenWidthDp.dp - 36.dp, 340.dp)
    val cardWidthPx = with(density) { floatingWidthDp.toPx() }
    val cardHeightPx = if (cardMeasuredSize.height > 0) cardMeasuredSize.height.toFloat() else with(density) { 96.dp.toPx() }

    val minX = with(density) { 12.dp.toPx() }
    val maxX = if (containerSize.width > 0) (containerSize.width - cardWidthPx - minX).coerceAtLeast(minX) else minX
    val minY = with(density) { 8.dp.toPx() }
    val maxY = if (containerSize.height > 0) (containerSize.height - cardHeightPx - with(density) { 72.dp.toPx() }).coerceAtLeast(minY) else minY

    val travelRangeX = (maxX - minX).coerceAtLeast(1f)
    val travelRangeY = (maxY - minY).coerceAtLeast(1f)

    LaunchedEffect(positionState, containerSize, configuration.screenWidthDp, configuration.screenHeightDp) {
        if (positionState.hasCustomPosition && containerSize.width > 0 && containerSize.height > 0) {
            if (!isMoveMode) {
                val targetX = minX + positionState.xRatio * travelRangeX
                val targetY = minY + positionState.yRatio * travelRangeY
                floatingOffset = Offset(targetX.coerceIn(minX, maxX), targetY.coerceIn(minY, maxY))
            }
        } else if (!positionState.hasCustomPosition && !isMoveMode) {
            floatingOffset = null
        }
    }

    val onEnterMoveModeFromInline: () -> Unit = {
        val root = rootLayoutCoordinates
        val inline = inlineSlotCoordinates
        val initialOffset = if (root != null && inline != null && inline.isAttached) {
            val posInRoot = root.localPositionOf(inline, Offset.Zero)
            Offset(posInRoot.x.coerceIn(minX, maxX), posInRoot.y.coerceIn(minY, maxY))
        } else {
            Offset(minX + 0.5f * travelRangeX, minY + 0.2f * travelRangeY)
        }
        floatingOffset = initialOffset
        isMoveMode = true
    }

    val onDragDelta: (Offset) -> Unit = { delta ->
        val current = floatingOffset ?: Offset(minX, minY)
        val newX = (current.x + delta.x).coerceIn(minX, maxX)
        val newY = (current.y + delta.y).coerceIn(minY, maxY)
        floatingOffset = Offset(newX, newY)
    }

    val saveFloatingPosition: () -> Unit = {
        val current = floatingOffset
        if (current != null) {
            val xRatio = ((current.x - minX) / travelRangeX).coerceIn(0f, 1f)
            val yRatio = ((current.y - minY) / travelRangeY).coerceIn(0f, 1f)
            DeepDiveManager.savePositionRatios(context, xRatio, yRatio)
        }
    }

    val onResetPosition: () -> Unit = {
        DeepDiveManager.clearCustomPosition(context)
        floatingOffset = null
        isMoveMode = false
    }

    val onDoneMoveMode: () -> Unit = {
        saveFloatingPosition()
        isMoveMode = false
    }

    BackHandler(enabled = isMoveMode) {
        onDoneMoveMode()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootLayoutCoordinates = coords
                containerSize = coords.size
            }
    ) {
        LazyColumn(
            modifier = Modifier
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
                                .background(MaterialTheme.colorScheme.primaryContainer)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        inlineSlotCoordinates = coords
                        if (coords.size.height > 0) {
                            inlineCardHeightDp = with(density) { coords.size.height.toDp() }
                        }
                    }
            ) {
                if (!isFloating) {
                    DeepDiveCard(
                        state = deepDiveState,
                        onStartSession = { endTimeMillis, style ->
                            DeepDiveManager.startSession(context, endTimeMillis, style)
                        },
                        onEndSession = {
                            DeepDiveManager.endSession(context)
                        },
                        isFloating = false,
                        isMoveMode = false,
                        onEnterMoveMode = onEnterMoveModeFromInline,
                        onDoneMoveMode = onDoneMoveMode,
                        onResetPosition = onResetPosition,
                        onDragDelta = onDragDelta,
                        onDragEnd = saveFloatingPosition
                    )
                } else {
                    DeepDiveDockedPlaceholder(
                        heightDp = inlineCardHeightDp,
                        onReset = onResetPosition
                    )
                }
            }
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
                                .background(MaterialTheme.colorScheme.primaryContainer)
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
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
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

    // Floating Deep Dive Overlay (sibling to LazyColumn, freely moves across entire screen)
    if (isFloating && floatingOffset != null) {
        val offset = floatingOffset!!
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                }
                .width(floatingWidthDp)
                .onGloballyPositioned { coords ->
                    cardMeasuredSize = coords.size
                }
                .zIndex(20f)
        ) {
            DeepDiveCard(
                state = deepDiveState,
                onStartSession = { endTimeMillis, style ->
                    DeepDiveManager.startSession(context, endTimeMillis, style)
                },
                onEndSession = {
                    DeepDiveManager.endSession(context)
                },
                modifier = Modifier.fillMaxWidth(),
                isFloating = true,
                isMoveMode = isMoveMode,
                onEnterMoveMode = {
                    isMoveMode = true
                },
                onDoneMoveMode = onDoneMoveMode,
                onResetPosition = onResetPosition,
                onDragDelta = onDragDelta,
                onDragEnd = saveFloatingPosition
            )
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
}

@Composable
private fun DeepDiveDockedPlaceholder(
    heightDp: androidx.compose.ui.unit.Dp,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onReset,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp, max = heightDp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Deep Dive is floating",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Text(
                text = "Tap to dock here",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = NudgeBlue,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

