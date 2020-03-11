/*
 * AirVision
 *
 * Copyright (c) LanternPowered <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision

import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.vector.Vector3d

/**
 * Represents a transform within a cardinal coordinate system.
 *
 * @property position The position (each component is in meters)
 * @property rotation The rotation
 */
data class Transform(
    val position: Vector3d,
    val rotation: Quaterniond
) {

  fun withPosition(position: Vector3d) = copy(position = position)

  fun withRotation(rotation: Quaterniond) = copy(rotation = rotation)

  companion object {

    val ORIGIN = Transform(Vector3d.ZERO, Quaterniond.IDENTITY)
  }
}
