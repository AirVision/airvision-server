plugins {
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
}

val ktor: (String) -> String by project

dependencies {
  implementation(project(":airvision-core"))
  // CSV Parser
  implementation(group = "com.github.doyaaaaaken", name = "kotlin-csv-jvm", version = "0.15.2")

  // Ktor
  implementation(ktor("server-netty"))

  // Database
  implementation(group = "org.jetbrains.exposed", name = "exposed", version = "0.17.7")
  implementation(group = "com.zaxxer", name = "HikariCP", version = "3.4.2")
  // PostgreSQL JDBC Driver
  implementation(group = "org.postgresql", name = "postgresql", version = "42.2.11")

  // ADS-B
  implementation(group = "org.opensky-network", name = "libadsb", version = "3.2.0")

  // Serial Communication
  implementation(group = "com.fazecast", name = "jSerialComm", version = "2.6.0")

  // Testing
  testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.2.0")
  testImplementation(kotlin(module = "test"))
  testImplementation(ktor("server-test-host"))
}

tasks {
  jar {
    exclude("log4j2.xml")
    rename("log4j2_prod.xml", "log4j2.xml")
    // Only enable async logging outside dev mode, using async in combination
    // with code location logging is disabled by default to avoid performance
    // issues, but in dev we want to see the locations, so no async here
    // See https://logging.apache.org/log4j/2.x/manual/async.html @ Location, location, location...
    rename("log4j2_prod.component.properties", "log4j2.component.properties")
  }
}
