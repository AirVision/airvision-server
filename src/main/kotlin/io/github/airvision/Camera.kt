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
import org.spongepowered.math.vector.Vector4d

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
      val near = 0.1
      val far = 1000.0
      val aspect = fov.x / fov.y
      /*
      // TODO: Why must fx and fy be inverted?
      val fx = 1.0 / tan(Math.toRadians(fov.x / 2.0))
      return Camera(Matrix4d(
          fx, 0.0, 0.0, 0.0,
          0.0, fx / aspect, 0.0, 0.0,
          0.0, 0.0, -far / (far - near), -(far * near) / (far - near),
          0.0, 0.0, -1.0, 0.0))
      */
      return Camera(Matrix4d.createPerspective(fov.x, aspect, near, far))
    }
  }
}

private const val Epsilon = 0.0001

private fun printVector(vector: Vector4d) {
  System.out.printf("(%f, %f, %f, %f)\n", vector.x, vector.y, vector.z, vector.w)
}

private fun printVector(vector: Vector3d) {
  System.out.printf("(%f, %f, %f)\n", vector.x, vector.y, vector.z)
}

private fun printVector(vector: Vector2d) {
  System.out.printf("(%f, %f)\n", vector.x, vector.y)
}

/**
 * Converts the 3d point to a 2d point within the camera view. Returns
 * null if the point isn't within the camera view.
 *
 * The returned position is within bounds [0,0] to [1,1]
 */
fun Vector3d.toViewPosition(camera: Camera): Vector2d? {
  // https://www.scratchapixel.com/lessons/3d-basic-rendering/perspective-and-orthographic-projection-matrix/building-basic-perspective-projection-matrix
  // with modifications

  var pos = camera.viewMatrix.transform(this)
  print("pos 1: ")
  printVector(pos)

  // Point is behind camera, so not visible
  if (pos.z < 0)
    return null

  pos = camera.projectionMatrix.transform(pos)
  print("pos 2: ")
  printVector(pos)

  val x = (-pos.x + 1.0) / 2.0
  val y = 1.0 - (pos.y + 1.0) / 2.0

  printVector(Vector2d(x, y))
  println()

  // Check whether the values are valid, using a epsilon
  // to fix errors on edge cases
  if (x !in -Epsilon..1.0 + Epsilon || y !in -Epsilon..1.0 + Epsilon)
    return null

  return Vector2d(
      x.coerceIn(0.0, 1.0),
      y.coerceIn(0.0, 1.0))
}

private fun Matrix4d.transform(vector: Vector3d): Vector3d {
  val transformed = this.transform(vector.toVector4(1.0))
  printVector(transformed)
  return if (transformed.w != 1.0) {
    transformed.toVector3().div(transformed.w)
  } else {
    transformed.toVector3()
  }
}
