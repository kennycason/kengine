plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
}

val kengineLibraryProjects = setOf(
    "kengine",
    "kengine-3d",
    "kengine-reactive",
    "kengine-test",
    "kengine-network",
    "kengine-physics",
    "kengine-sound"
)

val kengineVersion = providers.gradleProperty("kengine.version")
    .orElse("0.1.0-SNAPSHOT")

val githubPackagesOwner = providers.gradleProperty("gpr.owner")
    .orElse(providers.environmentVariable("GITHUB_REPOSITORY_OWNER"))
    .orElse("kennycason")
    .map { it.lowercase() }

val githubPackagesRepository = providers.gradleProperty("gpr.repository")
    .orElse(providers.environmentVariable("GITHUB_REPOSITORY").map { it.substringAfter("/") })
    .orElse("kengine")

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    if (name in kengineLibraryProjects) {
        group = "io.github.kennycason.kengine"
        version = kengineVersion.get()

        plugins.apply("maven-publish")

        extensions.configure<org.gradle.api.publish.PublishingExtension>("publishing") {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/${githubPackagesOwner.get()}/${githubPackagesRepository.get()}")
                    credentials {
                        username = providers.gradleProperty("gpr.user")
                            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                            .orElse("")
                            .get()
                        password = providers.gradleProperty("gpr.key")
                            .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                            .orElse("")
                            .get()
                    }
                }
            }

            publications.withType<org.gradle.api.publish.maven.MavenPublication>().configureEach {
                pom {
                    name.set(project.name)
                    description.set("Kengine Kotlin Multiplatform module: ${project.name}")
                    url.set("https://github.com/kennycason/kengine")
                    scm {
                        url.set("https://github.com/kennycason/kengine")
                        connection.set("scm:git:https://github.com/kennycason/kengine.git")
                        developerConnection.set("scm:git:ssh://git@github.com/kennycason/kengine.git")
                    }
                }
            }
        }
    }
}
