/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision

import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.matrix.Matrix4d
import org.spongepowered.math.vector.Vector2d
import org.spongepowered.math.vector.Vector3d

/**
 * Represents the state of a camera.
 *
 * @property projectionMatrix The projection matrix
 * @property viewMatrix The view matrix
 * @property transform The transform of the camera
 */
data class Camera(
    val projectionMatrix: Matrix4d,
    val transform: Transform = Transform.ORIGIN
) {

  private val inverseRotationMatrix: Matrix4d by lazy {
    Matrix4d.createRotation(transform.rotation.invert())
  }

  val viewMatrix: Matrix4d by lazy {
    val rotationMatrix = inverseRotationMatrix
    val positionMatrix = Matrix4d.createTranslation(transform.position.negate())
    rotationMatrix.mul(positionMatrix)
  }

  /**
   * Gets a new [Camera] state with the given [Transform].
   */
  fun withTransform(transform: Transform) = copy(transform = transform)

  /**
   * Rotates this camera with the given [rotation] [Quaterniond].
   */
  fun rotate(rotation: Quaterniond) = withTransform(transform.rotate(rotation))

  /**
   * Rotates this camera with the given [translation] [Vector3d].
   */
  fun translate(translation: Vector3d) = withTransform(transform.translate(translation))

  /**
   * Gets the y axis, relative to this camera.
   */
  val yAxis: Vector3d by lazy { toCamera(Vector3d.UNIT_Y) }

  /**
   * Gets the y axis, relative to this camera.
   */
  val xAxis: Vector3d by lazy { toCamera(Vector3d.UNIT_X) }

  /**
   * Gets the z axis, relative to this camera.
   */
  val zAxis: Vector3d by lazy { toCamera(Vector3d.UNIT_Z) }

  private fun toCamera(vector: Vector3d): Vector3d {
    return inverseRotationMatrix.transform(vector.toVector4(1.0)).toVector3()
  }

  companion object {

    /**
     * Creates a new [Camera] from the given [fov], where the x component
     * represents the horizontal and y the vertical FOV (field of view).
     */
    fun ofPerspective(fov: Vector2d): Camera {
      val near = 0.1
      val far = 1000.0
      val aspect = fov.x / fov.y
      return Camera(Matrix4d.createPerspective(fov.x, aspect, near, far))
    }
  }
}

private const val Epsilon = 0.00001

/**
 * Converts the 3d point to a 2d point within the camera view. Returns
 * null if the point isn't within the camera view.
 *
 * The returned position is within bounds [0,0] to [1,1]
 */
fun Vector3d.toViewPosition(camera: Camera): Vector2d? {
  // Camera Coordinate System
  // https://www.scratchapixel.com/images/upload/perspective-matrix/camera.png

  var pos = camera.viewMatrix.transform(this)
  // Point is behind the camera, so not visible
  if (pos.z > 0)
    return null

  pos = camera.projectionMatrix.transform(pos)

  // -1.0..1.0 -> 0.0..1.0
  // 0.0 is left, 1.0 is right
  val x = (pos.x + 1.0) / 2.0
  // -1.0..1.0 -> 1.0..0.0
  // 0.0 is top, 1.0 is bottom
  val y = 1.0 - (pos.y + 1.0) / 2.0

  // Check whether the values are valid, using a epsilon
  // to fix errors on edge cases
  if (x !in -Epsilon..1.0 + Epsilon || y !in -Epsilon..1.0 + Epsilon)
    return null

  return Vector2d(
      x.coerceIn(0.0, 1.0),
      y.coerceIn(0.0, 1.0))
}

private fun Matrix4d.transform(vector: Vector3d): Vector3d {
  val transformed = transform(vector.toVector4(1.0))
  return if (transformed.w != 1.0) {
    transformed.toVector3().div(transformed.w)
  } else {
    transformed.toVector3()
  }
}
