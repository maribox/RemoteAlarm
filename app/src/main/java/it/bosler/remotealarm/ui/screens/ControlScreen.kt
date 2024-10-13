package it.bosler.remotealarm.ui.screens

import android.R.attr.angle
import android.R.attr.padding
import android.R.attr.radius
import android.R.attr.value
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.bosler.remotealarm.ui.viewmodel.ControlViewModel
import java.nio.file.Files.size
import kotlin.math.*

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ControlScreen(
    viewModel: ControlViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var intensity by remember{ mutableStateOf(.3f) }
    var cw_ww_balance by remember { mutableStateOf(.5f) }
    Column(modifier = Modifier.fillMaxSize()) {
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        Column (modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(.3f))
            Row(Modifier.fillMaxWidth().weight(.6f)) {
                Box(Modifier.height(500.dp).width(500.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.clip(CircleShape).clickable(onClick = { intensity = (if (intensity == 0f) 1f else 0f)}).padding(50.dp) ) {
                        Text(
                            String.format("%.0f%%", intensity * 100),
                            fontSize = 40.sp,
                        )
                    }
                    CircularSlider(
                        value = intensity,
                        onValueChange = { intensity = it },
                        progressColor = Color(0xf0f5b21e),
                        backgroundColor = Color(0xff14314d),
                        thumbColor = Color.Transparent,
                        stroke = 30f,
                        touchStroke = 500f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Box(Modifier.weight(.1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                GradientSlider(
                    value = cw_ww_balance,
                    onValueChange = { cw_ww_balance = it },
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

@Composable
fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush,
    thumbColor: Color
) {
    Box (contentAlignment = Alignment.Center){
        CustomSliderTrack(gradient, modifier = modifier.height(4.dp).fillMaxSize())
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = modifier.height(4.dp).fillMaxSize()
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
    onValueChange: ((Float)->Unit)? = null
){
    var angle by remember { mutableStateOf(-60f) }
    var last by remember { mutableStateOf(0f) }
    var radius by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }

    var appliedAngle by remember { mutableStateOf(value * 300f) }
    LaunchedEffect(key1 = value) {
        appliedAngle = value * 300f
    }

    LaunchedEffect(key1 = angle){
        var a = angle
        a += 60
        if(a<=0f){
            a += 360
        }
        a = a.coerceIn(0f,300f)
        if(last<150f&&a==300f){
            a = 0f
        }
        last = a
        appliedAngle = a
    }
    LaunchedEffect(key1 = appliedAngle){
        onValueChange?.invoke(appliedAngle/300f)
    }
    Canvas(
        modifier = modifier
            .onGloballyPositioned {
                center = Offset(it.size.width / 2f, it.size.height / 2f)
                radius = min(it.size.width.toFloat(), it.size.height.toFloat()) / 2f - padding - stroke/2f
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
    ){
        drawArc(
            color = backgroundColor,
            startAngle = -240f,
            sweepAngle = 300f,
            topLeft = center - Offset(radius,radius),
            size = Size(radius*2,radius*2),
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
            topLeft = center - Offset(radius,radius),
            size = Size(radius*2,radius*2),
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
                radius*cos((120+appliedAngle)*PI/180f).toFloat(),
                radius*sin((120+appliedAngle)*PI/180f).toFloat()
            )
        )
    }
}

fun angle(center: Offset, offset: Offset): Float {
    val rad = atan2(center.y - offset.y, center.x - offset.x)
    val deg = Math.toDegrees(rad.toDouble())
    return deg.toFloat()
}

fun distance(first: Offset, second: Offset) : Float{
    return sqrt((first.x-second.x).square()+(first.y-second.y).square())
}

fun Float.square(): Float{
    return this*this
}