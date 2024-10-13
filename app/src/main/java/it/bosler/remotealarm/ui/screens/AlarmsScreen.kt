package it.bosler.remotealarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.bosler.remotealarm.data.Alarms.ScheduleType
import it.bosler.remotealarm.ui.components.AlarmCardList
import it.bosler.remotealarm.ui.viewmodel.AlarmViewModel
import it.bosler.remotealarm.ui.viewmodel.AlarmsScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    viewModel: AlarmViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        AlarmCardList(alarms = state.alarms, onToggleAlarm = viewModel::toggleAlarm)
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
                println("Adding new alarm ${state.alarms.size}")
                viewModel.openNewAlarm()
            },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Alarm")
        }
        if (state.isAlarmEditOpen) {
            AlarmForm(state, viewModel)
        }
    }
}

@Composable
private fun AlarmForm(
    state: AlarmsScreenState,
    viewModel: AlarmViewModel
) {
    val alarm = state.currentEditedAlarm!!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(
                onClick = { println("test") },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Edit Alarm",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(0.5f),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                var expanded by rememberSaveable { mutableStateOf(false) }
                Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
                    Box(contentAlignment = Alignment.Center) {
                        val text = stringResource(alarm.schedule.scheduleType.uiNameResId)
                        Text(
                            text,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(10.dp)
                                )
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                                .clickable { expanded = true }
                        )
                        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                ScheduleType.entries.forEach { scheduleType ->
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(scheduleType.uiNameResId)) },
                                        onClick = {
                                            viewModel.changeCurrentAlarmType(scheduleType)
                                            expanded = false
                                        })
                                }
                            }
                        }
                    }
                }
            }
            Divider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }



        Column (modifier = Modifier.height(50.dp)) {
            Divider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 0.dp)
            )
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .height(40.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.Right,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.closeCurrentAlarm() }) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Button(onClick = { viewModel.saveCurrentAlarm() }) {
                    Text("Save")
                }
            }
        }
    }
}