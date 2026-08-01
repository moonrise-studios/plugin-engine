pluginManagement {
    repositories {
        maven("https://repo1.maven.org/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "plugin-engine"
include("common")
include("paper")
include("bungeecord")
include("velocity")
