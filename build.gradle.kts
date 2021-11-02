import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import com.matthewprenger.cursegradle.Options
import com.modrinth.minotaur.TaskModrinthUpload
import com.modrinth.minotaur.request.Dependency.DependencyType
import com.modrinth.minotaur.request.VersionType

plugins {
    kotlin("jvm") version "1.5.21"
    id("fabric-loom") version "0.9.+"
    id("maven-publish")
    id("io.gitlab.arturbosch.detekt") version "1.18.1"
    id("com.github.jakemarsden.git-hooks") version "0.0.2"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.modrinth.minotaur") version "1.2.1"
    id("com.matthewprenger.cursegradle") version "1.4.0"
}

var release = false
val props = properties

val modId: String by project
val modName: String by project
val modVersion: String by project
val mavenGroup: String by project

base.archivesBaseName = modId
version = "$modVersion${getVersionMetadata()}"
group = mavenGroup

sourceSets {
    create("testmod") {
        runtimeClasspath += sourceSets["main"].runtimeClasspath
        compileClasspath += sourceSets["main"].compileClasspath
    }
}

loom {
    runs {
        create("testmodClient") {
            client()
            ideConfigGenerated(project.rootProject == project)
            name("Networking Testmod (Client)")
            source(sourceSets.getByName("testmod"))
        }
    }
}

configurations.implementation.get().extendsFrom(configurations.shadow.get())

fun DependencyHandlerScope.modImplementationAndInclude(dep: Any) {
    modImplementation(dep)
    include(dep)
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.nucleoid.xyz/")
    maven("https://maven.bymartrixx.me/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    mavenCentral()
    jcenter()
}

dependencies {
    // To change the versions see the libs.versions.toml

    // Fabric
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Fabric API
    modImplementation(libs.fabric.api)

    // Permissions
    modImplementationAndInclude(libs.fabric.permissions)

    // Translations
    modImplementationAndInclude(libs.translations)

    // Kotlin
    modImplementation(libs.fabric.kotlin)

    // Database
    shadow(libs.exposed.core)
    shadow(libs.exposed.dao)
    shadow(libs.exposed.jdbc)
    shadow(libs.exposed.java.time)
    shadow(libs.sqlite.jdbc)

    // Config
    shadow(libs.konf.core)
    shadow(libs.konf.toml)

    // Debug
    modRuntime(libs.wdmcf)
}

tasks {
    val javaVersion = JavaVersion.VERSION_16

    processResources {
        inputs.property("id", modId)
        inputs.property("name", modName)
        inputs.property("version", version)

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to modId,
                    "version" to version,
                    "name" to modName,
                    "fabricLoader" to libs.versions.fabric.loader.get(),
                    "fabricApi" to libs.versions.fabric.api.get(),
                    "fabricKotlin" to libs.versions.fabric.kotlin.get(),
                )
            )
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions { jvmTarget = javaVersion.toString() }
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }

    jar {
        from("LICENSE")
    }

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }

    remapJar {
        dependsOn(shadowJar)
        input.set(shadowJar.get().archiveFile)
    }

    shadowJar {
        from("LICENSE")

        configurations = listOf(
            project.configurations.shadow.get()
        )
        archiveClassifier.set("dev-all")

        exclude("kotlin/**", "kotlinx/**", "javax/**", "META-INF")
        exclude("org/checkerframework/**", "org/intellij/**", "org/jetbrains/annotations/**")
        exclude("com/google/gson/**")
        exclude("net/kyori/**")
        exclude("org/slf4j/**")

        val relocPath = "com.github.quiltservertools.libs."
        relocate("com.fasterxml", relocPath + "com.fasterxml")
        relocate("com.moandjiezana.toml", relocPath + "com.moandjiezana.toml")
        relocate("com.uchuhimo.konf", relocPath + "com.uchuhimo.konf")
        relocate("javassist", relocPath + "javassist")
        // Relocate each apache lib separately as just org.apache.commons will relocate things that aren't shadowed and break stuff
        relocate("org.apache.commons.lang3", relocPath + "org.apache.commons.lang3")
        relocate("org.apache.commons.text", relocPath + "org.apache.commons.text")
        relocate("org.reflections", relocPath + "org.reflections")
        // it appears you cannot relocate sqlite due to the native libraries
        // relocate("org.sqlite", relocPath + "org.sqlite")
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
        }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            // add all the jars that should be included when publishing to maven
            artifact(tasks.jar) {
                builtBy(tasks.remapJar)
            }
            artifact(tasks.getByName("sourcesJar")) {
                builtBy(tasks.remapSourcesJar)
            }
        }
    }

    // select the repositories you want to publish to
    repositories {
        // mavenLocal()
    }
}

curseforge {
    System.getenv("CURSEFORGE_TOKEN")?.let { CURSEFORGE_API ->
        apiKey = CURSEFORGE_API

        project(closureOf<CurseProject> {
            id = "491137"
            releaseType = "release"

            changelogType = "markdown"
            changelog = System.getenv("CHANGELOG")

            addGameVersion(libs.versions.minecraft.get())
            addGameVersion("Fabric")
            addGameVersion("Java 16")

            mainArtifact(tasks["remapJar"])

            relations(closureOf<CurseRelation> {
                props["cfReqDeps"].toString().split(",").forEach {
                    requiredDependency(it)
                }
            })
        })

        options(closureOf<Options> {
            forgeGradleIntegration = false
        })
    }
}

tasks {
    register<TaskModrinthUpload>("publishModrinth") {
        onlyIf { System.getenv().contains("MODRINTH_TOKEN") }
        dependsOn("build")

        group = "upload"

        token = System.getenv("MODRINTH_TOKEN")

        projectId = "LVN9ygNV"
        versionType = VersionType.RELEASE
        version = modVersion
        changelog = System.getenv("CHANGELOG")

        addGameVersion(libs.versions.minecraft.get())
        addLoader("fabric")

        props["mrReqDeps"].toString().split(",").forEach {
            addDependency(it, DependencyType.REQUIRED)
        }

        uploadFile = remapJar.get().archiveFile.get()
    }
}

tasks.register("release") {
    release = true
    dependsOn("curseforge", "publishModrinth")
}

detekt {
    buildUponDefaultConfig = true
    autoCorrect = true
    config = rootProject.files("detekt.yml")
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "detekt")
    )
}

fun getVersionMetadata(): String {
    if (release) return ""

    val buildId = System.getenv("GITHUB_RUN_NUMBER")

    // CI builds only
    if (buildId != null) {
        return "+build.$buildId"
    }

    // No tracking information could be found about the build
    return ""
}
