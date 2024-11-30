package it.bosler.remotealarm.ui.screens

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.juul.kable.Bluetooth
import com.juul.kable.Bluetooth.Availability.Available
import com.juul.kable.Bluetooth.Availability.Unavailable
import com.juul.kable.State
import it.bosler.remotealarm.bluetooth.ScanStatus.Scanning
import it.bosler.remotealarm.ui.viewmodel.ControlViewModel
import kotlin.math.*


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ControlScreen(
    viewModel: ControlViewModel,
) {
    val bluetooth = Bluetooth.availability.collectAsState(initial = null).value
    val state by viewModel.lightState.collectAsState()
    var scanPaneExpanded = viewModel.uiState.collectAsState().value.scanPaneExpanded

    LaunchedEffect(Unit) {
        Log.d("ControlScreen", "Tried to connect")
        viewModel.tryConnect()
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier
            .fillMaxHeight(if (scanPaneExpanded) .9f else .2f)
            .fillMaxWidth()
            .zIndex(1f)) {
            ScanPane(viewModel, bluetooth)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)) {
            Spacer(Modifier.weight(.3f))
            Row(Modifier
                .weight(.7f)) {
                Box(Modifier
                    .height(500.dp)
                    .width(500.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = {
                                viewModel.setIntensity(if (state.intensity == 0.0) 1.0 else 0.0) })
                            .padding(50.dp)
                    ) {
                        Text(
                            String.format("%.0f%%", state.intensity * 100),
                            fontSize = 40.sp,
                        )
                    }
                    CircularSlider(
                        value = state.intensity.toFloat(),
                        onValueChange = {
                            viewModel.setIntensity(it.toDouble())
                        },
                        progressColor = Color(0xf0f5b21e),
                        backgroundColor = Color(0xff14314d),
                        stroke = 30f,
                        touchStroke = 500f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Box(Modifier
                .weight(.1f)
                .fillMaxWidth()
                , contentAlignment = Alignment.Center) {
                GradientSlider(
                    value = state.cw_ww_balance.toFloat(),
                    onValueChange = { viewModel.setCW_WW_Balance(it.toDouble()) },
                    gradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFF9FD),
                            Color(0xFFFFB46B),
                        )
                    ),
                    thumbColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(333.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ScanPane(
    viewModel: ControlViewModel,
    bluetooth: Bluetooth.Availability?,
) {
    ProvideTextStyle(
        TextStyle(color = contentColorFor(backgroundColor = Color.Cyan))
    ) {
        val permissionsState = rememberMultiplePermissionsState(Bluetooth.permissionsNeeded)

        var didAskForPermission by remember { mutableStateOf(false) }
        if (!didAskForPermission) {
            didAskForPermission = true
            SideEffect {
                permissionsState.launchMultiplePermissionRequest()
            }
        }

        if (permissionsState.allPermissionsGranted) {
            PermissionGranted(viewModel, bluetooth)
        } else {
            if (permissionsState.shouldShowRationale) {
                BluetoothPermissionsNotGranted(permissionsState)
            } else {
                //BluetoothPermissionsNotAvailable({MainActivity.openAppDetails()})
                Log.d("ControlScreen", "Bluetooth permission not available")
            }
        }
    }
}


@Composable
private fun PermissionGranted(
    viewModel: ControlViewModel,
    bluetooth: Bluetooth.Availability?,
) {
    var collapsedCardHeight by remember { mutableStateOf(0.dp) }
    val scanPaneExpanded = viewModel.uiState.collectAsState().value.scanPaneExpanded
    val density = LocalDensity.current
    when (bluetooth) {
        Available -> {
            LaunchedEffect(scanPaneExpanded) {
                if (scanPaneExpanded) {
                    viewModel.startScan()
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = (if (scanPaneExpanded) Arrangement.Top else Arrangement.Center),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .onGloballyPositioned {
                        if (!scanPaneExpanded) {
                            collapsedCardHeight = with(density) {
                                it.size.height.toDp()
                            }
                        }
                    }) {
                Row(Modifier
                    .clickable { viewModel.onScanPaneClicked() }
                    .padding(16.dp)
                    .height(collapsedCardHeight - 32.dp)
                    .fillMaxWidth()) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                            // show current connection status, name, and indicator light (green, yellow or red)
                            Column (verticalArrangement = Arrangement.SpaceAround, modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                /*if (!connectionState) {
                                    Text("Connecting...", fontSize = 20.sp)
                                    Spacer(Modifier.height(8.dp))
                                }*/
                                Row(Modifier
                                    .width(200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    var (text, color) = when (viewModel.connectionState?.collectAsState()?.value ?: State.Disconnected(null)) {
                                        is State.Disconnected ->  "Disconnected" to Color.Gray
                                        is State.Connecting -> "Connecting..." to Color.Yellow
                                        is State.Connected ->  (viewModel.connectedPeripheralFlow.collectAsState().value?.name?: "Connected") to Color.Green
                                        is State.Disconnecting -> "Disconnecting..." to Color.Gray
                                    }
                                    Text(text, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.BluetoothSearching, contentDescription = "Connection Indicator", tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                    }
                }
                if (scanPaneExpanded) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        if (viewModel.status.collectAsState().value == Scanning) {
                            Loading()
                        }
                        viewModel.compatibleAdvertisements.collectAsState().value.forEach {
                            Box (
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable(onClick = { viewModel.connect(it) })
                            ) {
                                Text(fontSize = 20.sp, text = "${it.name}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text("Other devices found:")
                        viewModel.incompatibleAdvertisements.collectAsState().value.forEach {
                            Box (
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Text(color = Color.Gray, fontSize = 20.sp, text = "${it.name}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // TODO following stuff should be moved into card
        is Unavailable -> {
            LaunchedEffect(Unit) {
                Log.d("ControlScreen", "Bluetooth unavailable")
            }
        }

        null -> Loading()
    }
}

@Composable
private fun Loading() {
    Column(
        Modifier
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}


// taken from kable sample project
val Bluetooth.permissionsNeeded: List<String> by lazy {
    when {
        // If your app targets Android 9 (API level 28) or lower, you can declare the ACCESS_COARSE_LOCATION permission
        // instead of the ACCESS_FINE_LOCATION permission.
        // https://developer.android.com/guide/topics/connectivity/bluetooth/permissions#declare-android11-or-lower
        SDK_INT <= P -> listOf(ACCESS_COARSE_LOCATION)

        // ACCESS_FINE_LOCATION is necessary because, on Android 11 (API level 30) and lower, a Bluetooth scan could
        // potentially be used to gather information about the location of the user.
        // https://developer.android.com/guide/topics/connectivity/bluetooth/permissions#declare-android11-or-lower
        SDK_INT <= R -> listOf(ACCESS_FINE_LOCATION)

        // If your app targets Android 12 (API level 31) or higher, declare the following permissions in your app's
        // manifest file:
        //
        // 1. If your app looks for Bluetooth devices, such as BLE peripherals, declare the `BLUETOOTH_SCAN` permission.
        // 2. If your app makes the current device discoverable to other Bluetooth devices, declare the
        //    `BLUETOOTH_ADVERTISE` permission.
        // 3. If your app communicates with already-paired Bluetooth devices, declare the BLUETOOTH_CONNECT permission.
        // https://developer.android.com/guide/topics/connectivity/bluetooth/permissions#declare-android12-or-higher
        else /* SDK_INT >= S */ -> listOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun BluetoothPermissionsNotGranted(permissions: MultiplePermissionsState) {
    ActionRequired(
        icon = Icons.Filled.LocationDisabled,
        contentDescription = "Bluetooth permissions required",
        description = "Bluetooth permissions are required for scanning. Please grant the permission.",
        buttonText = "Continue",
        onClick = permissions::launchMultiplePermissionRequest,
    )
}

@Composable
private fun BluetoothPermissionsNotAvailable(openSettingsAction: () -> Unit) {
    ActionRequired(
        icon = Icons.Filled.Warning,
        contentDescription = "Bluetooth permissions required",
        description = "Bluetooth permission denied. Please, grant access on the Settings screen.",
        buttonText = "Open Settings",
        onClick = openSettingsAction,
    )
}

@Composable
private fun ActionRequired(
    icon: ImageVector,
    contentDescription: String?,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(150.dp),
            tint = contentColorFor(backgroundColor = Color.Magenta),
            imageVector = icon,
            contentDescription = contentDescription,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            text = description,
        )
        Spacer(Modifier.size(15.dp))
        Button(onClick) {
            Text(buttonText)
        }
    }
}


@Composable
fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush,
    thumbColor: Color
) {
    Box(contentAlignment = Alignment.Center) {
        CustomSliderTrack(gradient, modifier = modifier
            .height(4.dp)
            .fillMaxSize())
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = modifier
                .height(4.dp)
                .fillMaxSize()
        )
    }
}

@Composable
fun CustomSliderTrack(gradient: Brush, modifier: Modifier) {
    Canvas(
        modifier = modifier
    ) {
        drawLine(
            brush = gradient,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CircularSlider(
    value: Float, // should be between 0 and 1
    modifier: Modifier = Modifier,
    padding: Float = 50f,
    stroke: Float = 50f,
    cap: StrokeCap = StrokeCap.Round,
    touchStroke: Float = 50f,
    progressColor: Color = Color.Black,
    backgroundColor: Color = Color.LightGray,
    onValueChange: ((Float) -> Unit)? = null
) {
    var radius by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .onGloballyPositioned {
                center = Offset(it.size.width / 2f, it.size.height / 2f)
                radius = min(
                    it.size.width.toFloat(),
                    it.size.height.toFloat()
                ) / 2f - padding - stroke / 2f
            }
            .pointerInteropFilter {
                val x = it.x
                val y = it.y
                val offset = Offset(x, y)
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val d = distance(offset, center)
                        val a = angle(center, offset)
                        if (d >= radius - touchStroke / 2f && d <= radius + touchStroke / 2f && a !in -120f..-60f) {
                            updateAngle(a, onValueChange)
                        } else {
                            return@pointerInteropFilter false
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        updateAngle(angle(center, offset), onValueChange)
                    }

                    else -> {
                        return@pointerInteropFilter false
                    }
                }
                return@pointerInteropFilter true
            }
    ) {
        drawArc(
            color = backgroundColor,
            startAngle = -240f,
            sweepAngle = 300f,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2),
            useCenter = false,
            style = Stroke(
                width = stroke,
                cap = cap
            )
        )
        drawArc(
            color = progressColor,
            startAngle = 120f,
            sweepAngle = value.coerceIn(0f, 1f) * 300f,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2),
            useCenter = false,
            style = Stroke(
                width = stroke,
                cap = cap
            )
        )
    }
}

private fun updateAngle(
    rawAngle: Float,
    onValueChange: ((Float) -> Unit)?
) {
    var adjustedAngle = (rawAngle + 60).let {
        if (it < -30f) it + 360 else it
    }.coerceIn(0f, 300f)

    onValueChange?.invoke(adjustedAngle / 300f)
}

fun angle(center: Offset, offset: Offset): Float {
    val rad = atan2(center.y - offset.y, center.x - offset.x)
    val deg = Math.toDegrees(rad.toDouble())
    return deg.toFloat()
}

fun distance(first: Offset, second: Offset): Float {
    return sqrt((first.x - second.x).square() + (first.y - second.y).square())
}

fun Float.square(): Float {
    return this * this
}

fun angleBetween(center: Offset, point: Offset): Float {
    val angle = Math.toDegrees(atan2(point.y - center.y, point.x - center.x).toDouble()).toFloat()
    return angle
}

fun Float.normalizeAngle(): Float {
    return (this + 360f) % 360f
}