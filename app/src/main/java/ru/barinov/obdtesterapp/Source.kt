package ru.barinov.obdtesterapp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

abstract class Source {

    companion object {
        private const val BUFFER_CAPACITY = 100
        private const val REPLAY = 1
    }

    val inputByteFlow: MutableSharedFlow<ByteArray> = MutableSharedFlow(REPLAY, BUFFER_CAPACITY, BufferOverflow.SUSPEND)

    val outputByteFlow: MutableSharedFlow<ByteArray> = MutableSharedFlow(REPLAY, BUFFER_CAPACITY, BufferOverflow.SUSPEND)

    abstract suspend fun observeByteCommands(scope: CoroutineScope)
}
