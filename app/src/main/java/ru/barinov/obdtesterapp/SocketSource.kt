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

    private val input = BufferedInputStream(socket.inputStream)
    private val output = BufferedOutputStream(socket.outputStream)

    init {
        scope.launch {
            observeByteCommands()
        }
        scope.launch {
            try {
                socket.connect()
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(context, "CANT CONNECT", Toast.LENGTH_LONG).show()
                }
            }
            readData(this)
        }
    }

    private suspend fun observeByteCommands() {
        outputByteFlow.onEach { sendToSource(it) }.collect()
    }

    private fun sendToSource(bytes: ByteArray) {
        Log.d("@@@", "WRITE")
        output.write(bytes)
    }

    /*
        Run this func only in coroutine or new thread
     */
    private suspend fun readData(job: CoroutineScope) {
        val localBuffer = ByteBuffer.allocate(8)
        Log.d("@@@", "method")
        while (job.isActive) {
            try {
                var readByte: Byte = 0
                while (socket.isConnected && readByte > -1) {
                    Log.d("@@@", "read")
                    readByte = input.read().toByte()
                Log.d("@@@", readByte.toString())
                if (readByte.toInt().toChar() == '>') {
                    break
                } else {
                    localBuffer.put(readByte)
                }
                    Log.d("@@@", "END LOOP")
            }
                val controlByte: Byte =-1
                if(readByte> 0 || readByte == controlByte ) {
                    sendToCommander(localBuffer)
                }
                if(!localBuffer.hasRemaining() || readByte <1){
                    localBuffer.flip()
                    localBuffer.clear()
                }

        } catch (e: Exception){
            // todo
        }
    }

}

private suspend fun sendToCommander(buffer: ByteBuffer) {
    buffer.flip()
    inputByteFlow.emit(buffer.array())
    buffer.clear()
}


}