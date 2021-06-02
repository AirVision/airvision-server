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

/**
 * Different weight categories
 * of an aircraft.
 */
enum class WeightCategory {
  /**
   * Ultralight, like gliders, etc.
   */
  Ultralight,

  /**
   * < 15500 lbs
   */
  Light,

  /**
   * 15500 to 75000 lbs
   */
  Normal,

  /**
   * 75000 to 300000 lbs
   */
  Heavy,

  /**
   * > 300000 lbs
   */
  VeryHeavy,
}
