import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.DeploymentValidation

plugins {
    id("java")
    id("maven-publish")
    id("com.vanniktech.maven.publish.base") version "0.36.0" apply false
}

subprojects {
    apply(plugin = "java")

    repositories {
        maven("https://repo1.maven.org/maven2/")
        mavenCentral()

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
        val sourceUrl = "https://github.com/moonrise-studios/plugin-engine"
        description = "Spring-based Minecraft plugin engine (${project.name})"

        configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                pom.scm {
                    url.set(sourceUrl)
                    connection.set("scm:git:$sourceUrl.git")
                    developerConnection.set("scm:git:ssh://git@github.com/moonrise-studios/plugin-engine.git")
                }
            }
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

        if (providers.gradleProperty("centralRelease").isPresent) {
            apply(plugin = "com.vanniktech.maven.publish.base")
            afterEvaluate {
                val publication = the<PublishingExtension>().publications.named<MavenPublication>("mavenJava").get()
                configure<MavenPublishBaseExtension> {
                    coordinates(publication.groupId, publication.artifactId, publication.version)
                }
            }
            configure<MavenPublishBaseExtension> {
                publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.PUBLISHED)
                signAllPublications()
            }
        }
    }
}
