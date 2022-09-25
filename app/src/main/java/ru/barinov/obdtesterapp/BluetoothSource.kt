package ru.barinov.obdtesterapp

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BluetoothSource(private val socket: BluetoothSocket) : Source() {

    companion object {
        private const val END_VALUE: Byte = -1
        private const val SPACE_BYTE_VALUE: Byte = 13
    }

    private val input = socket.inputStream
    private val output = socket.outputStream

    override suspend fun observeByteCommands(scope: CoroutineScope) {
        scope.launch {
            outputByteFlow.onEach { sendToSource(it) }.collect()
        }
        scope.launch {
            readData(this)
        }
    }

    private suspend fun sendToSource(bytes: ByteArray) {
        if (socket.isConnected) {
            try {
                output.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun readData(job: CoroutineScope) {
        while (job.isActive) {
            var localBuffer: ByteBuffer? = null
            try {
                val capacity = input.available()
                while (socket.isConnected && capacity > 0) {
                    localBuffer = ByteBuffer.allocate(capacity)
                    localBuffer.order(ByteOrder.BIG_ENDIAN)
                    val readUByte = input.read().toByte()
                    if (readUByte.toChar() == '>') {
                        localBuffer.put(readUByte)
                        break
                    } else if (readUByte == END_VALUE) {
                        break
                    } else {
                        localBuffer.put(readUByte)
                    }
                }
                localBuffer?.let {
                    if (!it.hasRemaining()) {
                        sendToCommander(it)
                    } else {
                        it.flip()
                        it.clear()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun sendToCommander(buffer: ByteBuffer) {
        buffer.flip()
        val array = buffer.array()
        val filteredArray = array.filter {
            it != SPACE_BYTE_VALUE
        }.toByteArray()
        inputByteFlow.emit(filteredArray)
        buffer.clear()
    }
}
