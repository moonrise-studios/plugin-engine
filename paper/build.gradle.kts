plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version("9.2.2")
}

var id = "plugin-engine-paper"
var domain = "gg.moonrise.engine"
var apiVersion = "1.5.2"

repositories {
    maven("https://repo1.maven.org/maven2/")
    mavenCentral()
}

dependencies {
    implementation(project(":common"))

    // Paper
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    // Cloud Command Framework
    compileOnly("org.incendo:cloud-annotations:2.0.0")
    compileOnly("org.incendo:cloud-paper:2.0.0-beta.10")

    // Moss
    compileOnly("gg.moonrise.moss:moss-common:1.2.2")
    compileOnly("gg.moonrise.moss:moss-paper:1.2.2")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.7")

    // Spring & Jakarta
    compileOnly("org.springframework:spring-context:6.2.13")
    compileOnly("jakarta.annotation:jakarta.annotation-api:3.0.0")

    // ConfigLib
    compileOnly("de.exlll:configlib-yaml:4.8.1")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.76.1")
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("net.kyori:adventure-text-serializer-plain:4.26.1")
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
