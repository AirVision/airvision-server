/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.util.math.component

import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.vector.Vector2d
import org.spongepowered.math.vector.Vector2i
import org.spongepowered.math.vector.Vector3d
import org.spongepowered.math.vector.Vector4d

inline val Vector2i.x: Int
  get() = x()

inline val Vector2i.y: Int
  get() = y()

inline val Vector2d.x: Double
  get() = x()

inline val Vector2d.y: Double
  get() = y()

inline val Vector3d.x: Double
  get() = x()

inline val Vector3d.y: Double
  get() = y()

inline val Vector3d.z: Double
  get() = z()

inline val Vector4d.x: Double
  get() = x()

inline val Vector4d.y: Double
  get() = y()

inline val Vector4d.z: Double
  get() = z()

inline val Vector4d.w: Double
  get() = w()

inline val Quaterniond.x: Double
  get() = x()

inline val Quaterniond.y: Double
  get() = y()

inline val Quaterniond.z: Double
  get() = z()

inline val Quaterniond.w: Double
  get() = w()
