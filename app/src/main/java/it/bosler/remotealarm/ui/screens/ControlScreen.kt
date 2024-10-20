package it.bosler.remotealarm.ui.screens

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.R.attr.angle
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import it.bosler.remotealarm.ui.viewmodel.ControlViewModel
import kotlin.math.*


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ControlScreen(
    viewModel: ControlViewModel = viewModel(),
) {
    val bluetooth = Bluetooth.availability.collectAsState(initial = null).value
    val state by viewModel.state.collectAsState()
    var scanPaneExpanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier
            .fillMaxHeight(if (scanPaneExpanded) .9f else .3f)
            .fillMaxWidth()
            .zIndex(1f)) {
            ScanPane(bluetooth, scanPaneExpanded, { scanPaneExpanded = it })
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .fillMaxSize()
            .zIndex(0f)) {
            Spacer(Modifier.weight(.3f))
            Row(Modifier
                .weight(.6f)) {
                Box(Modifier
                    .height(500.dp)
                    .width(500.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = { viewModel.setIntensity(if (state.intensity == 0f) 1f else 0f) })
                            .padding(50.dp)
                    ) {
                        Text(
                            String.format("%.0f%%", state.intensity * 100),
                            fontSize = 40.sp,
                        )
                    }
                    CircularSlider(
                        value = state.intensity,
                        onValueChange = { viewModel.setIntensity(it) },
                        progressColor = Color(0xf0f5b21e),
                        backgroundColor = Color(0xff14314d),
                        thumbColor = Color.Transparent,
                        stroke = 30f,
                        touchStroke = 500f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Box(Modifier
                .weight(.1f)
                .fillMaxWidth(), contentAlignment = Alignment.Center) {
                GradientSlider(
                    value = state.cw_ww_balance,
                    onValueChange = { viewModel.setCW_WW_Balance(it) },
                    gradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFF9FD),
                            Color(0xFFFFB46B),
                        )
                    ),
                    thumbColor = Color.DarkGray,
                    modifier = Modifier.width(333.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ScanPane(
    bluetooth: Bluetooth.Availability?,
    scanPaneExpanded: Boolean,
    onExpandToggle: (Boolean) -> Unit
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
            PermissionGranted(bluetooth, scanPaneExpanded, onExpandToggle)
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
    bluetooth: Bluetooth.Availability?,
    scanPaneExpanded: Boolean,
    onExpandToggle: (Boolean) -> Unit,
    connectionState: Boolean = true
) {
    var collapsedCardHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    when (bluetooth) {
        Available -> {
            OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .onGloballyPositioned {
                        if (!scanPaneExpanded) {
                            collapsedCardHeight = with(density) {
                                it.size.height.toDp()
                            }
                        }
                    }) {
                Row(Modifier
                    .padding(16.dp)
                    .fillMaxWidth()) {
                    Column(Modifier.weight(.85f)) {
                        Box(Modifier.height(collapsedCardHeight - 32.dp)) {
                            Card(modifier = Modifier.fillMaxSize()) {
                                // show current connection status, name, and indicator light (green, yellow or red)
                                Column (verticalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    if (!connectionState) {
                                        Text("Connecting...", fontSize = 20.sp)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Row(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("Device Name", fontSize = 20.sp)
                                        Box(
                                            Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Color.Green)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Indicator Light", fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                        }
                        if (scanPaneExpanded) {
                            Column {

                            }
                        }
                    }
                    Column(
                        Modifier
                            .clickable { onExpandToggle(!scanPaneExpanded) }
                            .weight(.15f)
                            .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Toggle Scan Pane",
                            modifier = Modifier
                                .offset(
                                    x = 8.dp,
                                    y = ((collapsedCardHeight- 30.dp - 32.dp) / 2), // -30 because of the icon itself and -32 because of padding
                                )
                                .height(30.dp)
                                .rotate(if (scanPaneExpanded) 90f else 270f)
                        )
                    }
                }
            }
        }

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
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = Center,
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
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = Center,
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
                .align(CenterHorizontally),
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
    thumbColor: Color = Color.Blue,
    progressColor: Color = Color.Black,
    backgroundColor: Color = Color.LightGray,
    onValueChange: ((Float) -> Unit)? = null
) {
    var angle by remember { mutableStateOf(-60f) }
    var last by remember { mutableStateOf(0f) }
    var radius by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }

    var appliedAngle by remember { mutableStateOf(value * 300f) }
    LaunchedEffect(key1 = value) {
        appliedAngle = value * 300f
    }

    LaunchedEffect(key1 = angle) {
        var a = angle
        a += 60
        if (a <= 0f) {
            a += 360
        }
        a = a.coerceIn(0f, 300f)
        if (last < 150f && a == 300f) {
            a = 0f
        }
        last = a
        appliedAngle = a
    }
    LaunchedEffect(key1 = appliedAngle) {
        onValueChange?.invoke(appliedAngle / 300f)
    }
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
                            angle = a
                        } else {
                            return@pointerInteropFilter false
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        angle = angle(center, offset)
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
            sweepAngle = appliedAngle,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2),
            useCenter = false,
            style = Stroke(
                width = stroke,
                cap = cap
            )
        )
        drawCircle(
            color = thumbColor,
            radius = stroke,
            center = center + Offset(
                radius * cos((120 + appliedAngle) * PI / 180f).toFloat(),
                radius * sin((120 + appliedAngle) * PI / 180f).toFloat()
            )
        )
    }
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