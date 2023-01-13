package com.recodigo.cursocorrutinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.E
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

    fun asyncFunction() {
        viewModelScope.launch {
            log("FIRST LOG")
            val deferredNumber1: Deferred<Double> = async {
                returnARandomNumber(1_500)
            }
            val deferredNumber2: Deferred<Double> = async {
                returnARandomNumber(2_500)
            }
            log("first: ${deferredNumber1.await()}, second: ${deferredNumber2.await()}")
            log("LAST LOG")
        }
    }

    private suspend fun returnARandomNumber(delay: Long): Double {
        delay(delay)
        return Math.random() * 10
    }

    // ERROR HANDLING ------------------------
    // FIRST EXAMPLE
    fun errorHandling() {
        viewModelScope.launch {
            try {
                errorFunction()
            } catch (e: Exception) {
                e.printStackTrace()
                // log exception to server...
                // tell the user an error occurred
            }
        }
    }
    // SECOND EXAMPLE
    fun errorHandling2() {
        viewModelScope.launch {
            try {
                errorFunction()
                nonErrorFunction()
            } catch (e: Exception) {
                e.printStackTrace()
                // log exception to server...
                // tell the user an error occurred
            }
        }
    }

    // THIRD EXAMPLE
    fun errorHandling3() {
        try {
            viewModelScope.launch {
                errorFunction()
            }
            viewModelScope.launch {
                errorFunction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // log exception to server...
            // tell the user an error occurred
        }
    }

    // THIRD EXAMPLE Continue
    fun errorHandling3Continue() {
        viewModelScope.launch {
            try {
                errorFunction()
            } catch (e: Exception) {
                e.printStackTrace()
                // log exception to server...
                // tell the user an error occurred
            }
        }
        viewModelScope.launch {
            try {
                nonErrorFunction()
            } catch (e: Exception) {
                // I know this won't crash
            }
        }
    }

    // FOURTH EXAMPLE
    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        throwable.printStackTrace()
        // log exception to server...
        // tell the user an error occurred
    }
    fun errorHandling4() {
        viewModelScope.launch(exceptionHandler) {
            errorFunction()
        }
        viewModelScope.launch(exceptionHandler) {
            nonErrorFunction()
        }
    }

    // FIFTH EXAMPLE
    fun errorHandling5() {
        viewModelScope.launch {
            val first: Deferred<Double> = async {
                errorFunctionForAsync()
            }
            val second: Deferred<Double> = async { nonErrorFunctionForAsync() }
            try {
                log("first: ${first.await()}, second: ${second.await()}")
            } catch (e: Exception) {
                e.printStackTrace()
                // log exception to server...
                // tell the user an error occurred
            }
        }
    }

    // FIFTH EXAMPLE continue
    fun errorHandling5Continue() {
        viewModelScope.launch {
            supervisorScope {
                val first: Deferred<Double> = async { errorFunctionForAsync() }
                val second: Deferred<Double> = async { nonErrorFunctionForAsync() }
                try {
                    log("first: ${first.await()}, second: ${second.await()}")
                } catch (e: Exception) {
                    e.printStackTrace()
                    // log exception to server...
                    // tell the user an error occurred
                }
            }
        }
    }

    private suspend fun errorFunction() {
        withContext(Dispatchers.IO) {
            log("STARTED errorFunction -----")
            delay(500)
            throw NullPointerException("My error")
        }
    }

    private suspend fun nonErrorFunction() {
        withContext(Dispatchers.IO) {
            log("STARTED nonErrorFunction")
            delay(1_500)
            log("FINISHED nonErrorFunction")
        }
    }

    private suspend fun errorFunctionForAsync(): Double {
        withContext(Dispatchers.IO) {
            log("STARTED errorFunction -----")
            delay(500)
            throw NullPointerException("My error")
        }
    }

    private suspend fun nonErrorFunctionForAsync(): Double {
        return withContext(Dispatchers.IO) {
            log("STARTED nonErrorFunction")
            delay(1_500)
            log("FINISHED nonErrorFunction")
            return@withContext 5.5
        }
    }

    // FIFTH VIDEO
    private val coroutineContext: CoroutineContext = Job() + Dispatchers.IO + CoroutineName("my name") + exceptionHandler
    private val coroutineScope = CoroutineScope(coroutineContext)

    @OptIn(ExperimentalStdlibApi::class)
    fun fifthVideo() {
        coroutineScope.launch(Dispatchers.Main) {
            log("NAME: ${this.coroutineContext[CoroutineName]} - ${this.coroutineContext[CoroutineDispatcher]}")
        }
        coroutineScope.launch {
            log("NAME: ${this.coroutineContext[CoroutineName]} - ${this.coroutineContext[CoroutineDispatcher]}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        coroutineScope.cancel()
    }
}
