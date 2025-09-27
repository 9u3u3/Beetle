package com.bsbeetle.bananaspaghetti.ui.keyboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

@SuppressLint("DefaultLocale")
@Composable
fun SmoothSlider(sliderViewModel: SliderViewModel) {
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()

    // Local temp state for smoother dragging
    var localValue by remember { mutableFloatStateOf(sliderValue) }

    // Raw input string for TextField
    var inputText by remember { mutableStateOf(sliderValue.toString()) }

    // Sync inputText when sliderValue changes externally
    LaunchedEffect(sliderValue) {
        inputText = String.format("%.2f", sliderValue)  // Sync text field value whenever slider value changes
        localValue = sliderValue    // Sync local value whenever ViewModel changes (for + / - buttons)
    }




    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Value: ", color = Color.White)
            OutlinedTextField(
                modifier = Modifier
                    .width(120.dp) ,  // shrink horizontally
                value = inputText,
                onValueChange = { newValue ->
                    inputText = newValue

                    // Only update ViewModel when valid Float
                    newValue.toFloatOrNull()?.let { num ->
                        sliderViewModel.setSliderValue(num)
                    }
                },
            )
        }

        Slider(
            value = localValue,
            onValueChange = { newVal -> localValue = newVal },
            onValueChangeFinished = { sliderViewModel.setSliderValue(localValue) },
            valueRange = 1f..49f
        )

        Row(
            modifier = Modifier.padding(top = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { sliderViewModel.decrement() },
                modifier = Modifier.padding(horizontal = 2.dp)
            ) { Text("-", color = Color.White, modifier = Modifier.align(Alignment.Bottom)) }

            Button(
                onClick = { sliderViewModel.increment() },
                modifier = Modifier.padding(horizontal = 2.dp)
            ) { Text("+", color = Color.White) }
        }

    }
}
