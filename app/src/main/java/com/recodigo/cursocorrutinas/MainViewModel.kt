package com.recodigo.cursocorrutinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class) // needed to use measureTime
class MainViewModel : ViewModel() {

    private fun log(msg: String) {
        println("Thread: ${Thread.currentThread().name} - $msg")
    }

    fun runFirst() {
        viewModelScope.launch(Dispatchers.Unconfined) {
            val time: Duration = measureTime {
                log("print this NOW")
                delay(1000L)
                log("print this after 1 second")
            }
            log("duration: ${time.inWholeMilliseconds}")
        }
        log("can you guess the order of printing?")
    }

    fun runSecond() {
        viewModelScope.launch {
            delay(2000L)
            log("third")
        }
        viewModelScope.launch {
            delay(1000L)
            log("second")
        }
        log("This goes first")
    }

    fun runThird() {
        val job: Job = viewModelScope.launch(Dispatchers.Unconfined){
            log("This goes first")
            suspended()
            log("This goes third")
        }
        log("I want to quit")
        job.cancel()
        log("This goes fourth")
    }

    private suspend fun suspended() {
        withContext(Dispatchers.Default) {
            repeat(1000000000) {
                ensureActive()
                val la = 5 * it // just a random action
            }
            log("This goes second")
        }
    }
}