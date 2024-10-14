package it.bosler.remotealarm

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothLeScanner: BluetoothLeScanner

   /* private val db by lazy {
        Room.databaseBuilder(
            applicationContext, AlarmDatabase::class.java, "alarms.db"
        ).build()
    }
*/
    /*private val viewModel by viewModels<AlarmViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return AlarmViewModel(db.dao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.getAdapter()
        if (bluetoothAdapter == null) {
            // TODO Device doesn't support Bluetooth, not implemented yet
        } else {
            Log.d("BluetoothInit", "Bluetooth supported. Trying to initialize connection.")
            bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner
            requestBluetoothPermissions()
        }
        setContent {
            RemoteAlarmApp()
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("BluetoothInit", "Requesting Bluetooth permissions with new API.")
            requestBluetoothNewApi.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            // TODO not yet implemented
        }
    }

    private val requestBluetoothNewApi =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var gotAllPermissions = true
            permissions.entries.forEach {
                Log.d("PermissionRequest", "${it.key} = ${it.value}")
                if (!it.value) {
                    gotAllPermissions = false
                }
            }
            if (gotAllPermissions) {
                Log.d("BluetoothInit", "Bluetooth permissions granted.")
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBTRequest.launch(enableBtIntent)
            } else {
                Log.d("BluetoothInit", "Bluetooth permissions not granted.")
            }
        }

    private var enableBTRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("BluetoothInit", "Bluetooth enabled.")
            // We can trigger the ble scan here
            bluetoothLeScanner.startScan(leScanCallback)
        }else{
            Log.d("BluetoothInit", "Bluetooth not enabled.")
            // TODO not yet implemented
        }
    }
    private val leDeviceListAdapter = LeDeviceListAdapter()
    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            leDeviceListAdapter.addDevice(result.device)
            leDeviceListAdapter.notifyDataSetChanged()
        }
    }
}
