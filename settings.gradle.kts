rootProject.name = "AirVision"

val prefix = rootProject.name.toLowerCase()
listOf("core", "server").forEach {
  include(it)
  project(":$it").name = "$prefix-$it"
}
