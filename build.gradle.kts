plugins {
    id("java")
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()

        // Negative Games
        maven("https://repo.negative.games/repository/maven-releases/")
        maven("https://repo.negative.games/repository/maven-snapshots/")

        // PaperMC
        maven("https://repo.papermc.io/repository/maven-public/")

        // HelpChat
        maven("https://repo.helpch.at/releases")
    }
}

tasks.withType<Jar>() {
    enabled = false
}
