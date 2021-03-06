package ru.barinov.obdtesterapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.barinov.obdtesterapp.Source
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer

@SuppressLint("MissingPermission")
class SocketSource(private val socket: BluetoothSocket, val context: Context) {

    val inputByteFlow: MutableSharedFlow<ByteArray> = MutableSharedFlow()

    val outputByteFlow: MutableSharedFlow<ByteArray> = MutableSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val input: InputStream = socket.inputStream
    private val output: OutputStream = socket.outputStream

    init {
        scope.launch {
            observeByteCommands()
        }
        scope.launch {
            readData(this)
        }
    }

    private suspend fun observeByteCommands() {
        outputByteFlow.onEach { sendToSource(it) }.collect()
    }

    private suspend fun sendToSource(bytes: ByteArray) {
        Log.d("@@@", "WRITE " + bytes.size.toString() + " bytes")
        output.write(bytes)
    }

    /**
    Run this func only in coroutine or new thread
     */
    private suspend fun readData(job: CoroutineScope) {
        val localBuffer = mutableListOf<Byte>()
        while (job.isActive) {
            try {
                var readByte: Byte = 0
                while (socket.isConnected && readByte > -1) {
                    Log.d("@@@", "read")
                    val readUByteOrEmpty = input.read()
                    readByte = readUByteOrEmpty.toByte()
                    Log.d("@@@", readByte.toString())
                    if (readUByteOrEmpty.toChar() == '>') {
                        readByte = -1
                        break
                    } else {
                        localBuffer.add(readByte)
                    }
                    Log.d("@@@", "END LOOP")
                }
                if(localBuffer.isNotEmpty()){
                    sendToCommander(localBuffer)
                }
            } catch (e: Exception) {
                // todo
            }
        }

    }

    private suspend fun sendToCommander(buffer: MutableList<Byte>) {
        val filtered = buffer.filter { it.toInt() != 13 }
        buffer.clear()
        inputByteFlow.emit(filtered.toByteArray())
    }

}