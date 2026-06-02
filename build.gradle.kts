plugins {
    id("java")
    id("maven-publish")
}

subprojects {
    apply(plugin = "java")

    repositories {
        maven("https://repo1.maven.org/maven2/")
        mavenCentral()

        // Moonrise
        maven("https://repo.moonrise.gg/repository/maven-releases/")
        maven("https://repo.moonrise.gg/repository/maven-snapshots/")

        // PaperMC
        maven("https://repo.papermc.io/repository/maven-public/")

        // HelpChat
        maven("https://repo.helpch.at/releases")
    }
}

tasks.withType<Jar>() {
    enabled = false
}


subprojects {
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "nexus"

                    val snapshotsUrl = findProperty("nexusSnapshotsUrl") as String? ?: "https://repo.moonrise.gg/repository/maven-snapshots"
                    val releasesUrl  = findProperty("nexusReleasesUrl")  as String? ?: "https://repo.moonrise.gg/repository/maven-releases"

                    val isRelease = (findProperty("isRelease") == "true")
                    url = uri(if (isRelease) releasesUrl else snapshotsUrl)

                    credentials {
                        username = findProperty("nexusUsername") as String?
                        password = findProperty("nexusPassword") as String?
                    }
                }
            }
        }
    }
}
