package io.github.airvision

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.vector.Vector2d
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min

class CameraDataProvider(
  private val cameraManager: CameraManager,
  private val sensorManager: SensorManager,
) {

  private val quaternionWXYZ = FloatArray(4)
  private var _estimatedAccuracy = -1.0

  private val sensorListener = object : SensorEventListener {

    override fun onSensorChanged(event: SensorEvent) {
      SensorManager.getQuaternionFromVector(quaternionWXYZ, event.values)

      if (event.values.size > AccuracyIndex) {
        val accuracy = event.values[AccuracyIndex].toDouble()
        _estimatedAccuracy = if (accuracy == -1.0) -1.0 else Math.toDegrees(accuracy)
      }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }
  }

  private var running = false
  private var rotationSensor: Sensor? = null

  init {
    // Initialize as identity quaternion
    quaternionWXYZ[0] = 1f
  }

  val estimatedAccuracy: Double
    get() = _estimatedAccuracy

  val rotation: Quaterniond
    get() = Quaterniond(
      quaternionWXYZ[1].toDouble(),
      quaternionWXYZ[2].toDouble(),
      quaternionWXYZ[3].toDouble(),
      quaternionWXYZ[0].toDouble())

  fun start() {
    if (running)
      return
    rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    running = sensorManager.registerListener(sensorListener,
      rotationSensor, SensorManager.SENSOR_DELAY_UI)
  }

  fun stop() {
    if (!running)
      return
    running = false
    sensorManager.unregisterListener(sensorListener)
  }

  fun getCameraFov(cameraId: String): Vector2d {
    // https://stackoverflow.com/questions/39965408/what-is-the-android-camera2-api-equivalent-of-camera-parameters-gethorizontalvie
    val info = cameraManager.getCameraCharacteristics(cameraId)

    val sensorSize = info.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
    val focalLengths = info.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    if (sensorSize == null || focalLengths == null)
      error("Essential camera info isn't available.")

    val focalLength = focalLengths[0]
    // Short edge of the screen
    val xSize = min(sensorSize.height, sensorSize.width).toDouble()
    // Longest edge of the screen
    val ySize = max(sensorSize.height, sensorSize.width).toDouble()

    val xFov = Math.toDegrees(2 * atan(xSize / (2 * focalLength)))
    val yFov = Math.toDegrees(2 * atan(ySize / (2 * focalLength)))

    return Vector2d(xFov, yFov)
  }

  companion object {

    private const val AccuracyIndex = 4
  }
}
