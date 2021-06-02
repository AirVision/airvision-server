plugins {
  id("com.android.application")
  id("kotlin-android")
  kotlin("plugin.serialization")
  kotlin("kapt")
}

val composeVersion = "1.0.0-beta08"

dependencies {
  implementation(project(":airvision-core"))

  implementation("androidx.core:core-ktx:1.6.0-beta02")
  implementation("androidx.appcompat:appcompat:1.4.0-alpha02")
  implementation("com.google.android.material:material:1.3.0")
  implementation("androidx.compose.compiler:compiler:$composeVersion")
  implementation("androidx.compose.runtime:runtime:$composeVersion")
  implementation("androidx.compose.ui:ui:$composeVersion")
  implementation("androidx.compose.material:material:$composeVersion")
  implementation("androidx.compose.material:material-icons-core:$composeVersion")
  implementation("androidx.compose.material:material-icons-extended:$composeVersion")
  implementation("androidx.compose.ui:ui-tooling:$composeVersion")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
  implementation("androidx.activity:activity-compose:1.3.0-beta01")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.2")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
  androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
}

android {
  compileSdk = 30

  buildFeatures {
    compose = true
  }

  defaultConfig {
    applicationId = project.group.toString()
    minSdk = 26
    targetSdk = 30
    versionName = project.version.toString()
    versionCode = encodeVersion(versionName!!)
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = "1.8"
  }

  composeOptions {
    kotlinCompilerExtensionVersion = composeVersion
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true // Enables code shrinking for the release build type.
      proguardFiles(
        getDefaultProguardFile("proguard-android.txt"),
        "proguard-rules.pro"
      )
    }
  }

  packagingOptions {
    resources.excludes.apply {
      add("META-INF/DEPENDENCIES")
      add("META-INF/LICENSE")
      add("META-INF/LICENSE.txt")
      add("META-INF/license.txt")
      add("META-INF/NOTICE")
      add("META-INF/NOTICE.txt")
      add("META-INF/notice.txt")
      add("META-INF/ASL2.0")
    }
  }
}

/**
 * Encodes a semantic string version into a single integer.
 *
 * Each major, minor and patch component uses 10 bits, and
 * 1 bit to specify a snapshot.
 */
fun encodeVersion(version: String): Int {
  val index =  version.indexOf("-SNAPSHOT")
  val snapshot = index != -1
  val parts = version.substring(0, index).split(".")
  val major = parts[0].toInt()
  val minor = parts[1].toInt()
  val patch = parts.getOrNull(2)?.toInt() ?: 0
  var packed = (major shl 20) or (minor shl 10) or patch
  if (snapshot)
    packed = packed or (1 shl 30)
  return packed
}
