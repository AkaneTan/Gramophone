package org.akanework.gramophone.ui.viewmodels

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel

class TimerViewModel : ViewModel() {
    var timer: CountDownTimer? = null
    var timerDuration = 0
}
