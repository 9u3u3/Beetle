package com.bsbeetle.bananaspaghetti.ui.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

@Composable
fun SmoothSlider(sliderViewModel: SliderViewModel) {
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()

    // Local temp state for smoother dragging
    var localValue by remember { mutableFloatStateOf(sliderValue) }

    // Sync local value whenever ViewModel changes (for + / - buttons)
    LaunchedEffect(sliderValue) {
        localValue = sliderValue
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Value: ${"%.2f".format(localValue)}")

        Slider(
            value = localValue,
            onValueChange = { newVal -> localValue = newVal },
            onValueChangeFinished = { sliderViewModel.setSliderValue(localValue) },
            valueRange = 1f..49f
        )

        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { sliderViewModel.decrement() },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) { Text("-") }

            Button(
                onClick = { sliderViewModel.increment() },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) { Text("+") }
        }
    }
}
