rootProject.name = "AirVision"

val prefix = rootProject.name.toLowerCase()
listOf("core", "app", "server").forEach {
  include(it)
  project(":$it").name = "$prefix-$it"
}

pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    google()
  }

  val kotlinVersion = "1.5.10"

  plugins {
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("android") version kotlinVersion

    id("org.cadixdev.licenser") version "0.6.0"
  }
}
