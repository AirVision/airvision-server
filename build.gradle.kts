plugins {
  java
  eclipse
  idea
  kotlin("jvm") version "1.3.70"
  kotlin("kapt") version "1.3.70"
  kotlin("plugin.serialization") version "1.3.70"
  id("net.minecrell.licenser") version "0.4.1"
}

defaultTasks("licenseFormat", "build")

group = "io.github.airvision"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven("https://repo.spongepowered.org/maven/")
  maven("https://kotlin.bintray.com/kotlinx/")
  maven("https://kotlin.bintray.com/ktor/")
  maven("https://oss.sonatype.org/content/groups/public")
  maven("https://dl.bintray.com/kotlin/exposed/")
  maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
}

dependencies {
  // Kotlin
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))

  // Coroutines
  val coroutinesVersion = "1.3.4"
  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = coroutinesVersion)
  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-jdk8", version = coroutinesVersion)

  // Serialization
  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = "0.20.0")

  // Arrow
  val arrowVersion = "0.10.4"
  fun arrow(module: String) = "io.arrow-kt:arrow-$module:$arrowVersion"

  implementation(arrow("core"))
  implementation(arrow("syntax"))
  kapt(arrow("meta"))

  // CSV Parser
  implementation(group = "com.github.doyaaaaaken", name = "kotlin-csv-jvm", version = "0.7.3")

  // General utilities
  implementation(group = "com.google.guava", name = "guava", version = "28.0-jre")

  // Networking
  implementation(group = "io.netty", name = "netty-all", version = "4.1.46.Final")

  val ktorVersion = "1.3.2"
  fun ktor(module: String) = "io.ktor:ktor-$module:$ktorVersion"

  implementation(ktor("serialization"))
  implementation(ktor("server-netty"))
  implementation(ktor("client-apache"))
  implementation(ktor("client-core"))
  implementation(ktor("client-serialization-jvm"))

  // Database
  implementation(group = "org.jetbrains.exposed", name = "exposed", version = "0.17.7")

  // Cache
  implementation(group = "com.github.ben-manes.caffeine", name = "caffeine", version = "2.8.1")

  // Logging
  val log4jVersion = "2.12.1"
  fun log4j(module: String) = "org.apache.logging.log4j:log4j-$module:$log4jVersion"

  implementation(log4j("core"))
  implementation(log4j("api"))
  implementation(log4j("iostreams"))
  implementation(group = "com.lmax", name = "disruptor", version = "3.4.2")

  // Math: Vectors, Quaterions, etc.
  implementation(group = "org.spongepowered", name = "math", version = "2.0.0-SNAPSHOT")

  // Testing
  testCompile(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.2.0")
  testCompile(kotlin(module = "test"))
  testCompile(ktor("server-test-host"))
}

tasks {
  val baseName = "airvision-server"

  jar {
    archiveBaseName.set(baseName)

    exclude("log4j2.xml")
    rename("log4j2_prod.xml", "log4j2.xml")
    // Only enable async logging outside dev mode, using async in combination
    // with code location logging is disabled by default to avoid performance
    // issues, but in dev we want to see the locations, so no async here
    // See https://logging.apache.org/log4j/2.x/manual/async.html @ Location, location, location...
    rename("log4j2_prod.component.properties", "log4j2.component.properties")
  }

  val javadocJar = create<Jar>("javadocJar") {
    archiveBaseName.set(baseName)
    archiveClassifier.set("javadoc")
    from(javadoc)
  }

  val sourceJar = create<Jar>("sourceJar") {
    archiveBaseName.set(baseName)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
  }

  assemble {
    dependsOn(sourceJar)
    dependsOn(javadocJar)
  }

  artifacts {
    archives(jar.get())
    archives(sourceJar)
    archives(javadocJar)
  }

  listOf(jar.get(), sourceJar, javadocJar).forEach {
    it.from(project.file("LICENSE.txt"))
  }

  test {
    useJUnitPlatform()
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().forEach {
    it.kotlinOptions.apply {
      jvmTarget = "1.8"
      languageVersion = "1.3"

      val args = mutableListOf<String>()
      args += "-Xjvm-default=enable"
      args += "-Xallow-result-return-type"

      fun useExperimentalAnnotation(name: String) {
        args += "-Xuse-experimental=$name"
      }

      fun enableLanguageFeature(name: String) {
        args += "-XXLanguage:+$name"
      }

      enableLanguageFeature("InlineClasses")
      enableLanguageFeature("NewInference")
      enableLanguageFeature("NonParenthesizedAnnotationsOnFunctionalTypes")

      useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
      useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
      useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
      useExperimentalAnnotation("kotlin.experimental.ExperimentalTypeInference")
      useExperimentalAnnotation("kotlin.time.ExperimentalTime")
      useExperimentalAnnotation("kotlinx.serialization.ImplicitReflectionSerializer")

      freeCompilerArgs = args
    }
  }
}

license {
  header = rootProject.file("HEADER.txt")
  newLine = false
  ignoreFailures = false
  sourceSets = project.sourceSets

  include("**/*.java")
  include("**/*.kt")

  ext {
    set("name", project.name)
    set("url", "https://www.github.com/AirVision")
    set("organization", "AirVision")
  }
}
