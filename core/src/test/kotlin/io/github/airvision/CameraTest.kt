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

import org.junit.jupiter.api.Test
import org.spongepowered.math.imaginary.Quaterniond
import org.spongepowered.math.vector.Vector2d
import org.spongepowered.math.vector.Vector3d

private const val Epsilon = 0.0001

class CameraTest {

  private fun assertSamePoint(expected: Vector2d, actual: Vector2d?) {
    check(actual != null) { "Expected $expected, but got $actual" }
    assert(actual.distanceSquared(expected) < Epsilon) { "Expected $expected, but got $actual" }
  }

  @Test
  fun `check within camera view 1`() {
    val camera = Camera.ofPerspective(Vector2d(90.0, 90.0))
      .withTransform(Transform(Vector3d.ZERO, Quaterniond.fromAngleDegAxis(45.0, Vector3d.UNIT_Y)))

    // Left
    assertSamePoint(Vector2d(0.0, 0.5), Vector3d(-1.0, 0.0, 0.0).toViewPosition(camera))
    // Center
    assertSamePoint(Vector2d(0.5, 0.5), Vector3d(-1.0, 0.0, -1.0).toViewPosition(camera))
    // Right
    assertSamePoint(Vector2d(1.0, 0.5), Vector3d(0.0, 0.0, -1.0).toViewPosition(camera))
  }

  @Test
  fun `check within camera view 2`() {
    val camera = Camera.ofPerspective(Vector2d(90.0, 90.0))
      .withTransform(Transform(Vector3d.ZERO, Quaterniond.fromAngleDegAxis(-45.0, Vector3d.UNIT_Y)))

    // Left
    assertSamePoint(Vector2d(0.0, 0.5), Vector3d(0.0, 0.0, -1.0).toViewPosition(camera))
    // Center
    assertSamePoint(Vector2d(0.5, 0.5), Vector3d(1.0, 0.0, -1.0).toViewPosition(camera))
    // Right
    assertSamePoint(Vector2d(1.0, 0.5), Vector3d(1.0, 0.0, 0.0).toViewPosition(camera))
  }

  @Test
  fun `check within camera view 3`() {
    val camera = Camera.ofPerspective(Vector2d(90.0, 90.0))
      .withTransform(Transform(Vector3d.ZERO, Quaterniond.IDENTITY))

    // Left
    assertSamePoint(Vector2d(0.0, 0.5), Vector3d(-1.0, 0.0, -1.0).toViewPosition(camera))
    // Center
    assertSamePoint(Vector2d(0.5, 0.5), Vector3d(0.0, 0.0, -1.0).toViewPosition(camera))
    // Right
    assertSamePoint(Vector2d(1.0, 0.5), Vector3d(1.0, 0.0, -1.0).toViewPosition(camera))
  }

  @Test
  fun `check within camera view 4`() {
    val camera = Camera.ofPerspective(Vector2d(90.0, 90.0))
      .withTransform(Transform(Vector3d.ZERO, Quaterniond.fromAngleDegAxis(45.0, Vector3d.UNIT_X)))

    // Bottom
    assertSamePoint(Vector2d(0.5, 1.0), Vector3d(0.0, 0.0, -1.0).toViewPosition(camera))
    // Center
    assertSamePoint(Vector2d(0.5, 0.5), Vector3d(0.0, 1.0, -1.0).toViewPosition(camera))
    // Top
    assertSamePoint(Vector2d(0.5, 0.0), Vector3d(0.0, 1.0, 0.0).toViewPosition(camera))
  }

  @Test
  fun `check within camera view 5`() {
    val camera = Camera.ofPerspective(Vector2d(90.0, 90.0))
      .withTransform(Transform(Vector3d.ZERO, Quaterniond.fromAngleDegAxis(135.0, Vector3d.UNIT_Y)))

    // Left
    assertSamePoint(Vector2d(0.0, 0.5), Vector3d(0.0, 0.0, 1.0).toViewPosition(camera))
    // Center
    assertSamePoint(Vector2d(0.5, 0.5), Vector3d(-1.0, 0.0, 1.0).toViewPosition(camera))
    // Right
    assertSamePoint(Vector2d(1.0, 0.5), Vector3d(-1.0, 0.0, 0.0).toViewPosition(camera))
  }

  @Test
  fun `check outside camera view 1`() {
    val camera = Camera.ofPerspective(Vector2d(90.0, 90.0))
      .withTransform(Transform(Vector3d.ZERO, Quaterniond.fromAngleDegAxis(45.0, Vector3d.UNIT_Y)))

    check(Vector3d(1.0, 0.0, 1.0).toViewPosition(camera) == null)
    check(Vector3d(0.5, 0.0, -0.5).toViewPosition(camera) == null)
  }
}
