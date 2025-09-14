package com.bsbeetle.bananaspaghetti.viewmodel


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SliderViewModel : ViewModel() {
    private val _sliderValue = MutableStateFlow(25f)
    val sliderValue: StateFlow<Float> = _sliderValue

    fun setSliderValue(value: Float) {
        _sliderValue.value = value.coerceIn(1f, 49f)
    }

    fun increment() {
        _sliderValue.value = (_sliderValue.value + 1).coerceAtMost(49f)
    }

    fun decrement() {
        _sliderValue.value = (_sliderValue.value - 1).coerceAtLeast(1f)
    }
}
