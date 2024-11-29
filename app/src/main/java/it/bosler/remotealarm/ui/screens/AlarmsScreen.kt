package it.bosler.remotealarm.ui.screens

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.icu.text.SimpleDateFormat
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import it.bosler.remotealarm.bluetooth.BluetoothManager
import it.bosler.remotealarm.shared.MAX_COLOR_TEMP
import it.bosler.remotealarm.shared.MIN_COLOR_TEMP
import it.bosler.remotealarm.shared.formatTwoDigits
import it.bosler.remotealarm.ui.components.AlarmCardList
import it.bosler.remotealarm.ui.viewmodel.AlarmViewModel
import it.bosler.remotealarm.ui.viewmodel.AlarmsScreenState
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    viewModel: AlarmViewModel,
) {
    val state by viewModel.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        AlarmCardList(alarms = state.alarms, onToggleAlarm = viewModel::toggleAlarm)
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
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
@Preview
private fun AlarmFormPreview() {
    val viewModel = AlarmViewModel(BluetoothManager())
    viewModel.openNewAlarm()
    var viewModelState by remember { mutableStateOf(viewModel.state.value) }
    AlarmForm(viewModelState, viewModel)
}

fun convertMillisToDate(UTCmillis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy")
    return formatter.format(Date(UTCmillis))
}

@OptIn(ExperimentalMaterial3Api::class)
object PresentOrFutureSelectableDates: SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis >= momentDayStartedHereUTC
    }

    private val momentDayStartedHereUTC: Long by lazy {
        calculateMomentDayStartedHereUTC()
    }

    private fun calculateMomentDayStartedHereUTC(): Long {
        val now = ZonedDateTime.now()
        val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        return startOfDay.toInstant().toEpochMilli()
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year >= LocalDate.now().year
    }
}

//TODO: Try to use ZonedDateTime as unified state for date and time
// (if date is modified, set date to the selected date and time to the current time, if time is modified, set only the time)
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmForm(
    state: AlarmsScreenState,
    viewModel: AlarmViewModel
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(true) }
    val datePickerState = rememberDatePickerState(
        // because of the following line, we are saving date separate in UTC and hour/minute until it is saved,
        // at which point we don't need to get the start of this date in UTC...
        // who thought of this API??
        initialSelectedDateMillis = state.dateStartUTC,
        selectableDates = PresentOrFutureSelectableDates)
    val timePickerState = rememberTimePickerState(state.hour, state.minute)

    val context = LocalContext.current
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

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
                /*var expanded by rememberSaveable { mutableStateOf(false) }
                Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
                    Box(contentAlignment = Alignment.Center,
                        //modifier = Modifier.clickable { expanded = true }
                        ) {
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
                }*/
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Box(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha=0.1f))
                .padding(16.dp)
                .clickable { showDatePicker = true }) {
                Text(
                    convertMillisToDate(state.dateStartUTC),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Box(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha=0.1f))
                .padding(16.dp)
                .clickable { showTimePicker = true }) {
                Text(
                    "${formatTwoDigits(timePickerState.hour)}:${formatTwoDigits(timePickerState.minute)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 50.sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.padding(32.dp))

            LaunchedEffect(null) {
                viewModel.changeCurrentAlarmAction(targetIntensity = 1.0, targetCW_WW_Balance = 0.5)
            }

            Row (modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f / 3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Gray)
                            .clickable { viewModel.changeCurrentAlarmAction(hasRamp = !state.alarmAction.hasRamp) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Ramp Icon",
                            tint = if (state.alarmAction.hasRamp) Color.White else Color.LightGray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ramp")
                }

                Column(
                    modifier = Modifier.weight(1f / 3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Blue)
                            .clickable { viewModel.changeCurrentAlarmAction(targetIntensity = 1.5 - state.alarmAction.targetIntensity) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Target Light Icon",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(String.format("%.0f%%", state.alarmAction.targetIntensity * 100))
                    Text(String.format("%.0fK", (MAX_COLOR_TEMP - MIN_COLOR_TEMP)*state.alarmAction.targetCW_WW_Balance + MIN_COLOR_TEMP))
                }

                Column(
                    modifier = Modifier.weight(1f / 3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Red)
                            .clickable { viewModel.changeCurrentAlarmAction(shouldBlink = !state.alarmAction.shouldBlink) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Blinking Icon",
                            tint = if (state.alarmAction.shouldBlink) Color.White else Color.LightGray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Blinking")
                }
            }


            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDatePicker = false
                                datePickerState.selectedDateMillis?.let {
                                    // get currently selected time and add it on top of the selected date, then set it as the new selected datetime
                                    viewModel.changeCurrentAlarmDateUTC(it)
                                }
                            }
                        ) {
                            Text("OK")
                        }
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                    ) {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                        )
                    }
                }
            }
            
            if (showTimePicker) {
                TimePickerDialog(
                    onCancel = { showTimePicker = false },
                    onConfirm = {
                        viewModel.changeCurrentAlarmTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                ) {
                    TimePicker(state = timePickerState)
                }
            }

        }
        Column (modifier = Modifier.height(80.dp)) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 0.dp)
            )
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.Right,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.closeCurrentAlarm() },
                    modifier = Modifier.fillMaxHeight()

                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Button(onClick = { viewModel.saveCurrentAlarm() },
                    modifier = Modifier.fillMaxHeight()) {
                    Text("Save")
                }
            }
        }
    }
}

// https://stackoverflow.com/a/75855505/14236974
@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onCancel
                    ) { Text("Cancel") }
                    TextButton(
                        onClick = onConfirm
                    ) { Text("OK") }
                }
            }
        }
    }
}