package ru.barinov.obdtesterapp

import android.*
import android.R
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.*
import ru.barinov.obdtesterapp.databinding.ActivityMainBinding
import java.security.Permission
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var deviceAddr: String? = null

    private var source: SocketSource? =null

    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("test006", "OKKK")
            val bt = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val devices = bt.adapter.bondedDevices
            showMyDialog(devices, bt)
        }else{
            //deny
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onInit()
    }

    private fun onInit() {
        val button = binding.connectButton
        button.setOnClickListener {
            onPressButton()
        }

    }

    private fun observeAnswers(source: SocketSource) {
        Log.d("@@@", "OBS")
        lifecycleScope.launchWhenStarted {
            source.inputByteFlow.onEach {
                fillBytes(it)
                fillDex(it)
            }.collect()
        }
    }

    private fun fillDex(bytes: ByteArray) {
        Log.d("@@@", "FILLDEX")
        val dexInput = binding.stringInput
        val date = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.MEDIUM
        ).format(System.currentTimeMillis())
        if(dexInput.text.isEmpty()){
            dexInput.text = "$date"
            bytes.forEach {
                dexInput.text = "${dexInput.text} ${it.toHex()}"
            }
        } else{
            dexInput.text = "${dexInput.text} \n \n"
            dexInput.text = "${dexInput.text} $date"
            bytes.forEach {
                dexInput.text = "${dexInput.text} ${it.toHex()}"
            }
        }
    }

    private fun fillBytes(bytes: ByteArray) {
        Log.d("@@@", "FILLB")
        val byteInput = binding.byteInput
        val date = DateFormat.getDateTimeInstance(
          DateFormat.MEDIUM, DateFormat.MEDIUM
        ).format(System.currentTimeMillis())
        if(byteInput.text.isEmpty()){
            byteInput.text = "$date"
            bytes.forEach {
                byteInput.text = "${byteInput.text} $it"
            }
        } else{
            byteInput.text = "${byteInput.text} \n \n"
            byteInput.text = "${byteInput.text} $date"
            bytes.forEach {
                byteInput.text = "${byteInput.text} $it"
            }
        }
    }


    private fun onPressButton() {
        onCheckPermission()
    }

    @SuppressLint("MissingPermission")
    private fun showMyDialog(devices: MutableSet<BluetoothDevice>, bt: BluetoothManager) {
        val addresses = mutableListOf<String>()
        val names = mutableListOf<String>()
        if (devices.size > 0) {
            for (device in devices) {
                names.add( "${device.name} \n ${device.address}")
                addresses.add(device.address)
            }
        }

        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)

        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            R.layout.select_dialog_singlechoice,
            names.toTypedArray()
        )

        alertDialog.setSingleChoiceItems(adapter, -1, DialogInterface.OnClickListener { dialog, which ->
            dialog.dismiss()
            val position: Int = (dialog as AlertDialog).listView.checkedItemPosition
            val deviceAddress: String = addresses[position]
            this.deviceAddr = deviceAddress
            deviceAddr?.let {
                val device = bt.adapter.getRemoteDevice(it)
                val uuid = UUID.randomUUID()
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                source = SocketSource(socket, this)
                source?.let { source->
                    Log.d("@@@", "SOURCE")
                    binding.sendButton.setOnClickListener {
                        Log.d("@@@", "ON SEND")
                        lifecycleScope.launchWhenStarted {
                            source.outputByteFlow.emit(binding.byteInput.text.toString().toByteArray(Charsets.US_ASCII))
                        }
                    }
                    observeAnswers(source)
                }
            }
        })
        alertDialog.setTitle("Select Bluetooth device")
        alertDialog.show()
    }

    private fun onCheckPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.d("@@@", "PERMNOTGR")
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else{
                Log.d("@@@", "PERMGR")
                val bt = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val devices = bt.adapter.bondedDevices
                showMyDialog(devices, bt)

            }
        }
        else{
            val bt = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if(bt.adapter.isEnabled){
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)
            }

        }

    }

}
fun Byte.toHex(): String{
    return Integer.toHexString(this.toUByte().toInt())
}