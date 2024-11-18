package org.openmw.utils

import android.app.Activity
import android.os.Build
import android.util.Log
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.*
import org.openmw.Constants
import org.openmw.fragments.LaunchDocumentTree
import org.openmw.fragments.ModsFragment
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import java.util.Locale

data class ModValue(
    val id: Int,
    val category: String,
    val value: String,
    val isChecked: Boolean,
    var originalIndex: Int
)

fun readModValues(): List<ModValue> {
    val values = mutableListOf<ModValue>()
    val validCategories = setOf("data", "content", "groundcover")

    File(Constants.USER_OPENMW_CFG).forEachLine { line ->
        val trimmedLine = line.trim()
        if ("=" in trimmedLine) {
            val isChecked = !trimmedLine.startsWith("#")
            val (category, value) = trimmedLine.removePrefix("#").split("=", limit = 2).map { it.trim() }
            if (category in validCategories) {
                values.add(ModValue(values.size, category, value, isChecked, values.size + 1))
            }
        }
    }
    return values
}

fun writeModValuesToFile(modValues: List<ModValue>, filePath: String, ignoreList: List<String>) {
    val file = File(filePath)

    // Read existing lines while maintaining order and categories
    val existingLines = mutableListOf<String>().apply {
        if (file.exists()) {
            file.forEachLine { line ->
                if (line.trim().isNotEmpty()) {
                    add(line.trim())
                }
            }
        }
    }

    val sortedModValues = modValues.sortedWith(compareBy({ it.category }, { it.originalIndex }))
    val categorizedModValues = sortedModValues.groupBy { it.category }
    val orderedCategories = listOf("data", "content", "groundcover")

    // Create a map to track already present lines per category
    val existingCategoryLines = orderedCategories.associateWith { category ->
        existingLines.filter { it.startsWith(category) }.toMutableList()
    }

    // Append new lines to existing category lines
    orderedCategories.forEach { category ->
        categorizedModValues[category]?.sortedBy { it.originalIndex }?.forEach { modValue ->
            val line = "${modValue.category}=${modValue.value}" // Ignore isChecked
            val lineWithoutPrefix = line.removePrefix("#")
            val duplicates = existingCategoryLines[category]?.map { it.removePrefix("#") } ?: emptyList()
            if (!duplicates.contains(lineWithoutPrefix) && !ignoreList.contains(modValue.value)) { // Add only if it's not a duplicate and not in ignore list
                existingCategoryLines[category]!!.add(line)
            }
        }
    }

    // Combine final lines preserving the order
    val finalLines = mutableListOf<String>()
    orderedCategories.forEach { category ->
        finalLines.addAll(existingCategoryLines[category]!!)
        finalLines.add("") // Add a newline between categories
    }

    // Write all lines back to the file
    file.writeText(finalLines.joinToString("\n"))
}


fun searchMods(query: String, modValues: List<ModValue>, category: String?): List<ModValue> {
    return modValues.filter {
        (category == null || it.category == category) && it.value.contains(query, ignoreCase = true)
    }
}

fun navigateToMod(
    modValue: ModValue,
    categorizedModValues: List<List<ModValue>>,
    setSelectedTabIndex: (Int) -> Unit,
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope
) {
    val tabIndex = categorizedModValues.indexOfFirst { categoryList -> categoryList.any { it.id == modValue.id } }
    if (tabIndex != -1) {
        setSelectedTabIndex(tabIndex)
        val itemIndex = categorizedModValues[tabIndex].indexOfFirst { it.id == modValue.id }
        if (itemIndex != -1) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(itemIndex)
            }
        }
    }
}



@DelicateCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun ModValuesList(modValues: List<ModValue>) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("data", "content", "groundcover")
    var categorizedModValues by remember {
        mutableStateOf(categories.map { category ->
            modValues.filter { it.category == category }
        })
    }
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var ModPath by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<ModValue>()) }
    var isFlashing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        var currentList = categorizedModValues[selectedTabIndex].toMutableList()
        val movedItem = currentList.removeAt(from.index)
        currentList.add(to.index, movedItem)

        categorizedModValues = categorizedModValues.toMutableList().apply {
            this[selectedTabIndex] = currentList
        }
    }
    val openDocumentTreeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    val modsFragment = ModsFragment()
                    modsFragment.modDocumentTreeSelection(context, uri) { modPath ->
                        ModPath = modPath
                    }
                }
            }
            // Reload the mod values and update the UI
            val newModValues = readModValues()
            categorizedModValues = categories.map { category ->
                newModValues.filter { it.category == category }
            }
        }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500
            ),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    Column {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(category.replaceFirstChar { it.titlecase(Locale.getDefault()) }) }
                )
            }
            Tab(
                selected = selectedTabIndex == categories.size,
                onClick = {
                    LaunchDocumentTree(openDocumentTreeLauncher, context) { modPath ->
                        ModPath = modPath
                    }
                },
                text = { Text("Add Mod") }
            )
            Tab(
                selected = selectedTabIndex == categories.size,
                onClick = { showDialog = true },
                icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
            )
            Tab(
                selected = false,
                onClick = { showSearchDialog = true },
                icon = { Icon(Icons.Default.Search, contentDescription = "Search Mods") }
            )
        }
        fun handleSearchResult(modValue: ModValue) {
            navigateToMod(
                modValue,
                categorizedModValues,
                setSelectedTabIndex = { selectedTabIndex = it },
                lazyListState = lazyListState,
                coroutineScope = coroutineScope  // Pass the CoroutineScope
            )
            showSearchDialog = false
        }
        if (showSearchDialog) {
            Dialog(onDismissRequest = { showSearchDialog = false }) {
                Card(
                    modifier = Modifier.background(
                        animateColorAsState(
                            targetValue = if (isFlashing) Color.Red else Color.White,  // Change colors as needed
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 500),
                                repeatMode = RepeatMode.Reverse
                            ), label = ""
                        ).value
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .height(400.dp),  // Increased height for filter options
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Search Mods", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Filter Checkboxes
                        categories.forEach { category ->
                            var isChecked by remember { mutableStateOf(false) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        isChecked = it
                                        selectedCategory = if (isChecked) category else null
                                        searchResults = searchMods(searchQuery, modValues, selectedCategory)
                                    }
                                )
                                Text(category)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                searchResults = searchMods(it, modValues, selectedCategory)
                            },
                            label = { Text("Search") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(searchResults) { result ->
                                Text(
                                    text = result.value,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable {
                                            handleSearchResult(result)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Show dialog when showDialog is true
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Card {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Import / Export options for using a")
                        Text("custom openmw.cfg.")
                        CfgImport()
                    }
                }
            }
        } else {
            // Update UI after dialog closes
            val newModValues = readModValues()
            categorizedModValues = categories.map { category ->
                newModValues.filter { it.category == category }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(categorizedModValues[selectedTabIndex], key = { it.originalIndex }) { modValue ->
                ReorderableItem(reorderableLazyListState, key = modValue.originalIndex) {
                    var isDragging by remember { mutableStateOf(false) }
                    var showPopup by remember { mutableStateOf(false) }
                    var showDialog2 by remember { mutableStateOf(false) }
                    var isChecked by remember { mutableStateOf(modValue.isChecked) }
                    var showDeleteConfirmation by remember { mutableStateOf(false) }
                    var showMoveDialog by remember { mutableStateOf(false) }
                    val backgroundColor by animateColorAsState(
                        when {
                            isDragging -> Color(0xFF8BC34A) // Color during drag
                            modValue.isChecked -> Color.DarkGray
                            else -> MaterialTheme.colorScheme.surface
                        }, label = ""
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = backgroundColor,
                        ),
                        modifier = Modifier.then(if (isDragging) Modifier.graphicsLayer(scaleX = pulse, scaleY = pulse) else Modifier),
                        elevation = CardDefaults.cardElevation(4.dp),
                        onClick = { showPopup = true },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    isChecked = checked
                                    var currentList = categorizedModValues[selectedTabIndex].toMutableList()
                                    val index = currentList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                    if (index != -1) {
                                        currentList[index] = currentList[index].copy(isChecked = checked)
                                    }
                                    categorizedModValues = categorizedModValues.toMutableList().apply {
                                        this[selectedTabIndex] = currentList
                                    }

                                    // Update file
                                    val file = File(Constants.USER_OPENMW_CFG)
                                    val existingLines = mutableListOf<String>()
                                    if (file.exists()) {
                                        file.forEachLine { line ->
                                            if (line.trim().isNotEmpty()) {
                                                existingLines.add(line.trim())
                                            }
                                        }
                                    }
                                    val updatedLines = existingLines.map { line ->
                                        if (line.contains(modValue.value)) {
                                            if (checked) {
                                                modValue.category + "=" + modValue.value
                                            } else {
                                                "#" + modValue.category + "=" + modValue.value
                                            }
                                        } else {
                                            line
                                        }
                                    }
                                    file.writeText(updatedLines.joinToString("\n") + "\n")

                                    // Reload the mod values and update the UI
                                    val newModValues = readModValues()
                                    categorizedModValues = categories.map { category ->
                                        newModValues.filter { it.category == category }
                                    }

                                }
                            )
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = modValue.value,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Load Order: ${modValue.originalIndex}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                IconButton(
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = {
                                            isDragging = true
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                vibrate(context)
                                            }
                                        },
                                        onDragStopped = {
                                            isDragging = false
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                vibrate(context)
                                            }

                                            var currentList = categorizedModValues[selectedTabIndex].toMutableList()

                                            // Update the original indexes based on current list order
                                            currentList.forEachIndexed { index, item ->
                                                currentList[index] = item.copy(originalIndex = index + 1)
                                            }

                                            categorizedModValues = categorizedModValues.toMutableList().apply {
                                                this[selectedTabIndex] = currentList
                                            }

                                            // Update the file with the new order
                                            val file = File(Constants.USER_OPENMW_CFG)
                                            val finalLines = categorizedModValues.flatten()
                                                .sortedWith(compareBy({ categories.indexOf(it.category) }, { it.originalIndex }))
                                                .map { modValue ->
                                                    if (modValue.isChecked) {
                                                        "${modValue.category}=${modValue.value}"
                                                    } else {
                                                        "#${modValue.category}=${modValue.value}"
                                                    }
                                                }
                                            file.writeText(finalLines.joinToString("\n") + "\n")

                                            // Reload the mod values and update the UI
                                            val newModValues = readModValues()
                                            categorizedModValues = categories.map { category ->
                                                newModValues.filter { it.category == category }
                                            }
                                        }
                                    ),
                                    onClick = { showDialog2 = true },
                                ) {
                                    Icon(Icons.Rounded.Menu, contentDescription = "Reorder")
                                }

                            }
                        }
                        if (showPopup) {
                            Popup(
                                alignment = Alignment.Center,
                                onDismissRequest = { showPopup = false } // Hide the popup when dismissed
                            ) {
                                Surface(
                                    modifier = Modifier.padding(16.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Choose an action for the selected mod.")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Name: ${modValue.value}") // Display mod name
                                        Text("Category: ${modValue.category}") // Display mod category
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                // Handle the switch category action
                                                val updatedList = categorizedModValues[selectedTabIndex].toMutableList()
                                                val index = updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                                if (index != -1) {
                                                    val newCategory = if (modValue.category == "content") "groundcover" else "content"
                                                    updatedList[index] = updatedList[index].copy(category = newCategory)
                                                    categorizedModValues = categorizedModValues.toMutableList().apply {
                                                        this[selectedTabIndex] = updatedList
                                                    }

                                                    // Update the file directly with the new category values
                                                    val file = File(Constants.USER_OPENMW_CFG)
                                                    val existingLines = mutableListOf<String>()
                                                    if (file.exists()) {
                                                        file.forEachLine { line ->
                                                            if (line.trim().isNotEmpty()) {
                                                                existingLines.add(line.trim())
                                                            }
                                                        }
                                                    }

                                                    // Update file with new categories
                                                    val finalLines = categorizedModValues.flatten().map { modValue ->
                                                        "${modValue.category}=${modValue.value}"
                                                    }
                                                    file.writeText(finalLines.joinToString("\n") + "\n")

                                                    // Reload the mod values and update the UI
                                                    val newModValues = readModValues()
                                                    categorizedModValues = categories.map { category ->
                                                        newModValues.filter { it.category == category }
                                                    }
                                                }
                                                showPopup = false
                                            }
                                        ) {
                                            Text("Switch to ${if (modValue.category == "content") "groundcover" else "content"}")
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                // Show the delete confirmation dialog
                                                showDeleteConfirmation = true
                                            }
                                        ) {
                                            Text("Delete")
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                // Show the move dialog
                                                showMoveDialog = true
                                            }
                                        ) {
                                            Text("Move")
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(onClick = { showPopup = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }

                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                title = { Text("Confirm Deletion") },
                                text = { Text("Are you sure you want to delete this mod?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            // Handle the delete action
                                            val updatedList = categorizedModValues[selectedTabIndex].toMutableList()
                                            val index = updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                            if (index != -1) {
                                                updatedList.removeAt(index)
                                                categorizedModValues = categorizedModValues.toMutableList().apply {
                                                    this[selectedTabIndex] = updatedList
                                                }

                                                // Update the file after deletion
                                                val file = File(Constants.USER_OPENMW_CFG)
                                                val finalLines = categorizedModValues.flatten().map { modValue ->
                                                    "${modValue.category}=${modValue.value}"
                                                }
                                                file.writeText(finalLines.joinToString("\n") + "\n")

                                                // Reload the mod values and update the UI
                                                val newModValues = readModValues()
                                                categorizedModValues = categories.map { category ->
                                                    newModValues.filter { it.category == category }
                                                }
                                            }
                                            showDeleteConfirmation = false
                                            showPopup = false
                                        }
                                    ) {
                                        Text("Yes")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showDeleteConfirmation = false }) {
                                        Text("No")
                                    }
                                }
                            )
                        }
                        if (showMoveDialog) {
                            // Get the initial index of the mod
                            val initialIndex = categorizedModValues[selectedTabIndex].indexOfFirst { it.originalIndex == modValue.originalIndex }
                            selectedIndex = initialIndex

                            AlertDialog(
                                onDismissRequest = { showMoveDialog = false },
                                title = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Mod Mover",
                                            color = Color.Green,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Select the new position for the mod:")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            // Add a NumberPicker for selecting the index number
                                            val context = LocalContext.current

                                            AndroidView(factory = {
                                                NumberPicker(context).apply {
                                                    minValue = 0
                                                    maxValue = categorizedModValues[selectedTabIndex].size - 1
                                                    value = initialIndex  // Set the initial value
                                                    wrapSelectorWheel = true
                                                    // Custom formatter to display index and mod value
                                                    setFormatter { index ->
                                                        val modValue = categorizedModValues[selectedTabIndex].getOrNull(index)?.value ?: ""
                                                        "$index = $modValue"
                                                    }
                                                    setOnValueChangedListener { _, _, newVal ->
                                                        selectedIndex = newVal
                                                    }
                                                }.also { numberPicker ->
                                                    // Set the layout parameters to make the NumberPicker wider
                                                    numberPicker.layoutParams = LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                                    ).apply {
                                                        weight = 1f
                                                    }

                                                    // Remove the grayed-out effect
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                        numberPicker.setSelectionDividerHeight(0)
                                                    }
                                                }
                                            })

                                        }

                                        // Keep the current mod value as the one you clicked on
                                        val currentModValue = modValue.value

                                        // Display the mod value above the selected index
                                        val aboveModValue = categorizedModValues[selectedTabIndex].getOrNull(selectedIndex - 1)?.value
                                        if (aboveModValue != null) {
                                            Text("Above Mod: $aboveModValue")
                                        }
                                        Text(
                                            text = "Selected Mod: $currentModValue",
                                            color = Color.Green,
                                            textDecoration = TextDecoration.Underline,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                confirmButton = {
                                    val isSamePosition = selectedIndex == initialIndex
                                    Button(
                                        onClick = {
                                            // Handle the move action if the positions are different
                                            if (!isSamePosition) {
                                                val updatedList = categorizedModValues[selectedTabIndex].toMutableList()
                                                val currentIndex = updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }

                                                if (currentIndex != -1) {
                                                    // Implement the logic to move the mod to the new position
                                                    val newPosition = selectedIndex
                                                    Log.d("MoveDialog", "Moving mod from $currentIndex to $newPosition")
                                                    val movedMod = updatedList.removeAt(currentIndex)
                                                    updatedList.add(newPosition, movedMod)

                                                    // Update originalIndex for all mods
                                                    updatedList.forEachIndexed { index, modValue ->
                                                        modValue.originalIndex = index
                                                        Log.d("MoveDialog", "Mod at index $index has originalIndex ${modValue.originalIndex}")
                                                    }

                                                    // Update the mod values
                                                    categorizedModValues = categorizedModValues.toMutableList().apply {
                                                        this[selectedTabIndex] = updatedList
                                                    }

                                                    // Update the file with the new positions
                                                    val file = File(Constants.USER_OPENMW_CFG)
                                                    val finalLines = categorizedModValues.flatten().map { modValue ->
                                                        "${modValue.category}=${modValue.value}"
                                                    }
                                                    Log.d("MoveDialog", "Writing to file: $finalLines")
                                                    file.writeText(finalLines.joinToString("\n") + "\n")

                                                    // Reload the mod values and update the UI
                                                    val newModValues = readModValues()
                                                    categorizedModValues = categories.map { category ->
                                                        newModValues.filter { it.category == category }
                                                    }
                                                    Log.d("MoveDialog", "Mod values reloaded")
                                                } else {
                                                    Log.d("MoveDialog", "Mod with value not found.")
                                                }
                                                showMoveDialog = false
                                                showPopup = false
                                            } else {
                                                Log.d("MoveDialog", "Selected index is the same as the current index.")
                                            }
                                        }
                                        ,
                                        enabled = !isSamePosition
                                    ) {
                                        Text("Move")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showMoveDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        if (showDialog2) {
                            AlertDialog(
                                onDismissRequest = { showDialog2 = false },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val updatedList =
                                                categorizedModValues[selectedTabIndex].toMutableList()
                                            val index =
                                                updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                            if (index != -1) {
                                                val newCategory =
                                                    if (modValue.category == "content") "groundcover" else "content"
                                                updatedList[index] =
                                                    updatedList[index].copy(category = newCategory)
                                                categorizedModValues =
                                                    categorizedModValues.toMutableList().apply {
                                                        this[selectedTabIndex] = updatedList
                                                    }

                                                // Update the file directly with the new category values
                                                val file = File(Constants.USER_OPENMW_CFG)
                                                val existingLines = mutableListOf<String>()
                                                if (file.exists()) {
                                                    file.forEachLine { line ->
                                                        if (line.trim().isNotEmpty()) {
                                                            existingLines.add(line.trim())
                                                        }
                                                    }
                                                }

                                                // Update file with new categories
                                                val finalLines =
                                                    categorizedModValues.flatten().map { modValue ->
                                                        "${modValue.category}=${modValue.value}"
                                                    }
                                                file.writeText(finalLines.joinToString("\n") + "\n")

                                                // Reload the mod values and update the UI
                                                val newModValues = readModValues()
                                                categorizedModValues = categories.map { category ->
                                                    newModValues.filter { it.category == category }
                                                }
                                            }
                                            showDialog2 = false
                                        }
                                    ) {
                                        Text("Yes")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showDialog2 = false }) {
                                        Text("No")
                                    }
                                },
                                title = { Text("Confirm Action") },
                                text = { Text("Are you sure you want to switch the category to ${if (modValue.category == "content") "groundcover" else "content"}?") }
                            )
                        }
                    }
                }
            }
        }
    }
}
