package it.bosler.remotealarm.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.bosler.remotealarm.ui.components.AlarmCardList
import it.bosler.remotealarm.ui.viewmodel.AlarmViewModel
import org.jetbrains.annotations.Async.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    viewModel: AlarmViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val alarm = state.currentEditedAlarm!!
    Box(modifier = Modifier.fillMaxSize()) {
        AlarmCardList(alarms = state.alarms, onToggleAlarm = viewModel::toggleAlarm )
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
                println("Adding new alarm ${state.alarms.size}")
                viewModel.openNewAlarm()
              },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary) {
            Icon(Icons.Filled.Add, contentDescription = "Add Alarm")
    }
        if (state.isAlarmCreationOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.closeCurrentAlarm() },
                title = { Text("Edit Alarm") },
                text = {
                    Column {
                        if (alarm.schedule is it.bosler.remotealarm.model.Alarms.Schedule.SpecificTimestamp) {
                            // Todo Timepickerstate rausfinden
                            TimePicker(
                                alarm.schedule.UTCtimestamp,
                                onTimeChanged = { newTime -> selectedTime = newTime })
                            DatePicker(alarm, onDateChanged = { newDate -> selectedDate = newDate })
                        }
                        Switch(
                            checked = state.currentEditedAlarm!!.enabled,
                            onCheckedChange = { isChecked -> viewModel.enableCurrentAlarm(isChecked) }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.saveCurrentAlarm() }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = { viewModel.closeCurrentAlarm() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}