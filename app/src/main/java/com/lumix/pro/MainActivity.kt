package com.lumix.pro

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import com.lumix.pro.camera.CameraController
import com.lumix.pro.camera.SensorsHelper
import com.lumix.pro.gallery.MediaRepository
import com.lumix.pro.ui.LumixMainContent
import com.lumix.pro.ui.theme.LumixProTheme
import com.lumix.pro.utils.BatteryThermalMonitor
import com.lumix.pro.utils.PermissionHandler
import com.lumix.pro.utils.StorageUtils
import com.lumix.pro.video.AudioLevelMeter
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private lateinit var controller: CameraController
    private lateinit var repository: MediaRepository
    private lateinit var sensors: SensorsHelper
    private lateinit var audioMeter: AudioLevelMeter
    private lateinit var batteryMonitor: BatteryThermalMonitor

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var hasCameraPermission = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[android.Manifest.permission.CAMERA] == true
        if (hasCameraPermission) {
            controller.bindUseCases()
            startSensors()
        }
        setContent { AppUi() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        controller = CameraController(this, this)
        repository = MediaRepository(this)
        sensors = SensorsHelper(this)
        batteryMonitor = BatteryThermalMonitor(this)
        audioMeter = AudioLevelMeter(activityScope) { rms -> controller.updateAudioLevel(rms) }

        sensors.onOrientationChanged = { roll, pitch ->
            controller.updateRollPitch(roll, pitch)
        }
        batteryMonitor.onThermalChanged = { info ->
            controller.updateSystem(
                storageMb = StorageUtils.report(this).freeMb,
                battery = batteryMonitor.readBattery().levelPercent,
                thermal = info.isThrottled
            )
        }

        hasCameraPermission = PermissionHandler.hasCameraPermission(this)
        if (hasCameraPermission) {
            controller.bindUseCases()
            startSensors()
        }

        setContent { AppUi() }

        if (!hasCameraPermission) {
            val needed = (PermissionHandler.REQUIRED_CAMERA_PERMISSIONS.toList() +
                PermissionHandler.mediaPermissions().toList() +
                PermissionHandler.OPTIONAL_LOCATION_PERMISSIONS.toList() +
                listOf(android.Manifest.permission.RECORD_AUDIO))
                .filter { !PermissionHandler.isGranted(this, it) }
                .toTypedArray()
            permissionLauncher.launch(needed)
        }

        activityScope.launch {
            while (isActive) {
                val rep = StorageUtils.report(this@MainActivity)
                val bat = batteryMonitor.readBattery()
                controller.updateSystem(rep.freeMb, bat.levelPercent, false)
                delay(12_000)
            }
        }
    }

    @Composable
    private fun AppUi() {
        LumixProTheme {
            LumixMainContent(
                controller = controller,
                repository = repository,
                hasCameraPermission = hasCameraPermission,
                onRequestPermission = {
                    permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
                }
            )
        }
    }

    private fun startSensors() {
        sensors.start()
        audioMeter.start()
        batteryMonitor.start()
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission) startSensors()
    }

    override fun onPause() {
        super.onPause()
        sensors.stop()
        audioMeter.stop()
        batteryMonitor.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.release()
        activityScope.cancel()
    }
}
