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

  val viewMatrix: Matrix4d by lazy {
    val rotationMatrix = Matrix4d.createRotation(transform.rotation.invert())
    val positionMatrix = Matrix4d.createTranslation(transform.position.negate())
    rotationMatrix.mul(positionMatrix)
  }

  /**
   * Gets a new [Camera] state with the given [Transform].
   */
  fun withTransform(transform: Transform) = copy(transform = transform)

  companion object {

    /**
     * Creates a new [Camera] from the given [fov], where the x component
     * represents the horizontal and y the vertical FOV (field of view).
     */
    fun ofPerspective(fov: Vector2d): Camera {
      val fovX = fov.x
      val aspect = fov.x / fov.y
      return Camera(Matrix4d.createPerspective(fovX, aspect, 1.0, Double.MAX_VALUE / 2.0))
    }
  }
}

/**
 * Converts the 3d point to a 2d point within the camera view. Returns
 * null if the point isn't within the camera view.
 *
 * The returned position is within bounds [0,0] to [1,1]
 */
fun Vector3d.toViewPosition(camera: Camera): Vector2d? {
  var pos = camera.viewMatrix.transform(toVector4(1.0))
  pos = camera.projectionMatrix.transform(pos)

  // https://stackoverflow.com/questions/7748357/converting-3d-position-to-2d-screen-position

  val x = (pos.x / pos.z + 1.0) / 2.0
  val y = (pos.y / pos.z + 1.0) / 2.0

  if (x !in 0.0..1.0 || y !in 0.0..1.0)
    return null

  return Vector2d(x, y)
}
