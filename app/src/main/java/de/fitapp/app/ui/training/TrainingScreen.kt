@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.fitapp.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.fitapp.app.data.entity.ExerciseLogEntity
import de.fitapp.app.data.entity.MuscleGroup
import de.fitapp.app.ui.AppViewModel
import de.fitapp.app.util.MuscleProgressResult
import de.fitapp.app.util.PROGRESS_WINDOW_DAYS
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.round

/** Rundet auf eine Nachkommastelle, z. B. für die Anzeige "5.6". */
private fun Float.formatRating(): String = (round(this * 10f) / 10f).toString()

/** 80.0 -> "80", 82.5 -> "82.5" */
private fun formatKg(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()

@Composable
fun TrainingScreen(viewModel: AppViewModel) {
    val logs by viewModel.exerciseLogs.collectAsState()
    val muscleProgress by viewModel.muscleProgress.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Training", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Maximalgewicht eintragen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { RadarCard(muscleProgress) }

            if (logs.isEmpty()) {
                item {
                    Text("Noch keine Einträge. Tippe auf +, um das Maximalgewicht deiner ersten Übung einzutragen.")
                }
            } else {
                items(logs, key = { it.id }) { log -> ExerciseRow(log, formatter) }
            }
        }
    }

    if (showAdd) {
        AddMaxWeightDialog(
            onDismiss = { showAdd = false },
            onSave = { name, group, weight ->
                viewModel.saveMaxWeight(name, group, weight)
                showAdd = false
            }
        )
    }
}

@Composable
private fun RadarCard(results: List<MuscleProgressResult>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Muskelgruppen-Bewertung", style = MaterialTheme.typography.titleMedium)
            Text(
                "Bewertung von 0 bis 10 im Vergleich zum geschätzten Altersdurchschnitt " +
                    "(5.0 = Durchschnitt für dein Alter, Geschlecht und Gewicht). Es zählt dein " +
                    "höchstes eingetragenes Maximalgewicht der letzten $PROGRESS_WINDOW_DAYS Tage. " +
                    "Grobe Schätzung, kein medizinischer Vergleichswert. Gruppen ohne Einträge " +
                    "zeigen „noch keine Daten“.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            if (results.isNotEmpty()) {
                MuscleRadarChart(results) { it.label() }
            }
            Spacer(Modifier.height(8.dp))
            results.forEach { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(r.group.label())
                    Text(
                        if (r.hasEnoughData) {
                            "${r.rating.formatRating()} / 10 (${r.estimatedOneRepMaxKg.toInt()} kg, Ø ${r.averageOneRepMaxKg.toInt()} kg)"
                        } else {
                            "noch keine Daten"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(log: ExerciseLogEntity, formatter: DateTimeFormatter) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${log.exerciseName} · ${log.muscleGroup.label()}", style = MaterialTheme.typography.titleSmall)
            // Ältere Einträge (mit mehreren Wiederholungen) werden weiterhin vollständig angezeigt.
            val weightText = if (log.reps > 1) {
                "${formatKg(log.weightKg)} kg × ${log.reps} Wdh. × ${log.sets} Sätze"
            } else {
                "Maximalgewicht: ${formatKg(log.weightKg)} kg"
            }
            Text(
                "$weightText · ${LocalDate.ofEpochDay(log.dateEpochDay).format(formatter)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AddMaxWeightDialog(
    onDismiss: () -> Unit,
    onSave: (String, MuscleGroup, Float) -> Unit
) {
    var selected by remember { mutableStateOf<Pair<MuscleGroup, String>?>(null) }
    var weight by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val weightValue = weight.replace(',', '.').toFloatOrNull()

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard {
            Column(Modifier.padding(16.dp)) {
                Text("Maximalgewicht eintragen", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selected?.let { "${it.second} (${it.first.label()})" } ?: "Übung wählen")
                    Spacer(Modifier.weight(1f))
                    Text("▾")
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Maximalgewicht (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Button(
                        onClick = {
                            val s = selected
                            val w = weightValue
                            if (s != null && w != null) onSave(s.second, s.first, w)
                        },
                        enabled = selected != null && weightValue != null && weightValue > 0f
                    ) { Text("Speichern") }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            current = selected?.second,
            onDismiss = { showPicker = false },
            onPick = { group, name ->
                selected = group to name
                showPicker = false
            }
        )
    }
}

@Composable
private fun ExercisePickerDialog(
    current: String?,
    onDismiss: () -> Unit,
    onPick: (MuscleGroup, String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
        ) {
            Column {
                Text(
                    "Übung wählen",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(bottom = 8.dp)) {
                    exerciseCatalog.forEach { (group, names) ->
                        stickyHeader {
                            Text(
                                group.label(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(names) { name ->
                            Text(
                                name,
                                fontWeight = if (name == current) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(group, name) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
