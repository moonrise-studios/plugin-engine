plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version("9.2.2")
}

var id = "plugin-engine-bungeecord"
var domain = "gg.moonrise.engine"
var apiVersion = "1.7.2"

repositories {
    maven("https://repo1.maven.org/maven2/")
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation(project(":common"))

    // BungeeCord
    compileOnly("net.md-5:bungeecord-api:1.21-R0.5-SNAPSHOT")

    // Cloud Command Framework
    compileOnly("org.incendo:cloud-annotations:2.0.0")
    compileOnly("org.incendo:cloud-bungee:2.0.0-beta.10")

    // Moss
    compileOnly("gg.moonrise.moss:moss-common:1.2.2")
    compileOnly("gg.moonrise.moss:moss-bungeecord:1.2.2")

    // Adventure
    compileOnly("net.kyori:adventure-api:4.26.1")
    compileOnly("net.kyori:adventure-text-minimessage:4.26.1")
    compileOnly("net.kyori:adventure-platform-bungeecord:4.4.1")

    // Spring & Jakarta
    compileOnly("org.springframework:spring-context:6.2.13")
    compileOnly("jakarta.annotation:jakarta.annotation-api:3.0.0")

    // Logging
    compileOnly("org.slf4j:slf4j-api:2.0.17")

    // ConfigLib
    compileOnly("de.exlll:configlib-yaml:4.8.1")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set(id)
    archiveVersion.set(apiVersion)
    archiveClassifier.set("")
}

tasks.named<Jar>("sourcesJar") {
    from(project(":common").extensions.getByType<SourceSetContainer>().named("main").map { it.allSource })
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["shadow"])
            artifact(tasks.named<Jar>("sourcesJar"))
            artifact(tasks.named<Jar>("javadocJar"))

            groupId = domain
            artifactId = id
            version = apiVersion

            pom {
                name.set(id)
                description.set(project.description)
                url.set("https://github.com/moonrise-studios/plugin-engine")

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("ericlmao")
                        name.set("Eric")
                    }
                }
            }
        }
    }
}
