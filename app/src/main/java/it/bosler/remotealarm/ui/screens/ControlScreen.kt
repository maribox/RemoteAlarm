package it.bosler.remotealarm.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.bosler.remotealarm.ui.viewmodel.ControlViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: ControlViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Controls")
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        Column (modifier = Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(500.dp)) {
                VerticalSlider(Modifier.weight(0.5f))
                VerticalSlider(Modifier.weight(0.5f))
            }
            Text(text = sliderPosition.toString())
        }

    }
}



@Composable
fun VerticalSlider(modifier: Modifier) {
    var sliderPosition by remember { mutableStateOf(0f) }

    Slider(
        value = sliderPosition,
        valueRange = 1f..10f,
        onValueChange = { sliderPosition = it },
        modifier = modifier.graphicsLayer {
            rotationZ = 270f
            transformOrigin = TransformOrigin(0f, 0f)
        }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight,
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
    )
}
