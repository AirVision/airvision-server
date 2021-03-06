buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:7.1.0-alpha01")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
  }
}

plugins {
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("org.cadixdev.licenser")
}

allprojects {
  group = "io.github.airvision"
  version = "1.0-SNAPSHOT"

  repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven("https://maven.google.com/")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://kotlin.bintray.com/kotlinx/")
    maven("https://kotlin.bintray.com/ktor/")
    maven("https://oss.sonatype.org/content/groups/public")
    maven("https://dl.bintray.com/kotlin/exposed/")
    maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
  }
}

val ktorVersion = "1.5.4"
val ktor: (String) -> String = { "io.ktor:ktor-$it:$ktorVersion" }

subprojects {
  apply(plugin = "org.cadixdev.licenser")

  ext.set(::ktor.name, ktor)

  defaultTasks("licenseFormat", "build")

  afterEvaluate {
    dependencies {
      // Kotlin
      implementation(kotlin("stdlib-jdk8"))
      implementation(kotlin("reflect"))

      // Coroutines
      val coroutinesVersion = "1.5.0-RC"
      implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-core",
        version = coroutinesVersion
      )
      implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-jdk8",
        version = coroutinesVersion
      )

      // Serialization
      implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-serialization-core",
        version = "1.2.0"
      )

      // Arrow
      val arrowVersion = "0.13.2"
      fun arrow(module: String) = "io.arrow-kt:arrow-$module:$arrowVersion"

      implementation(arrow("core"))
      // kapt(arrow("meta"))

      // Networking
      implementation(group = "io.netty", name = "netty-all", version = "4.1.63.Final")

      implementation(ktor("serialization"))
      implementation(ktor("client-apache"))
      implementation(ktor("client-core"))
      implementation(ktor("client-serialization-jvm"))

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
      testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-engine",
        version = "5.2.0"
      )
      testImplementation(kotlin(module = "test"))
    }

    tasks {
      withType<Test> {
        useJUnitPlatform()
      }

      withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().forEach {
        it.kotlinOptions.apply {
          jvmTarget = "1.8"
          languageVersion = "1.5"

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
          useExperimentalAnnotation("kotlinx.serialization.UnstableDefault")
          useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
          useExperimentalAnnotation("kotlinx.serialization.ImplicitReflectionSerializer")
          useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
          useExperimentalAnnotation("io.ktor.util.KtorExperimentalAPI")
          useExperimentalAnnotation("kotlinx.coroutines.DelicateCoroutinesApi")
          useExperimentalAnnotation("kotlinx.coroutines.InternalCoroutinesApi")

          freeCompilerArgs = args
        }
      }
    }

    license {
      newLine(false)
      ignoreFailures(false)
      header(rootProject.file("HEADER.txt"))

      include("**/*.java")
      include("**/*.kt")

      properties {
        set("name", rootProject.name)
        set("url", "https://www.github.com/AirVision")
        set("organization", "AirVision")
      }
    }
  }
}

