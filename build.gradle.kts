plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.loom)
    alias(libs.plugins.detekt)
    alias(libs.plugins.git.hooks)
    alias(libs.plugins.shadow)
    `maven-publish`
}

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
    //serverOnlyMinecraftJar()

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
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    mavenCentral()
    mavenLocal()
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

    detektPlugins(libs.detekt.formatting)
}

tasks {
    val javaVersion = JavaVersion.VERSION_21

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
                    "fabricApi" to libs.versions.fabric.api.get(),
                    "fabricKotlin" to libs.versions.fabric.kotlin.get(),
                    "minecraft" to libs.versions.minecraft.get(),
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

    compileKotlin {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
        }
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

        exclude("kotlin/**", "kotlinx/**", "javax/**")
        exclude("org/checkerframework/**", "org/intellij/**", "org/jetbrains/annotations/**")
        exclude("com/google/gson/**")
        exclude("net/kyori/**")
        exclude("org/slf4j/**")

        val relocPath = "com.github.quiltservertools.libs."
        relocate("com.fasterxml", relocPath + "com.fasterxml")
        relocate("com.moandjiezana.toml", relocPath + "com.moandjiezana.toml")
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
            from(components["java"])
        }
    }

    // select the repositories you want to publish to
    repositories {
        // mavenLocal()
    }
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
    val buildId = System.getenv("GITHUB_RUN_NUMBER")
    val workflow = System.getenv("GITHUB_WORKFLOW")

    if (workflow == "Release") {
        return ""
    }

    // CI builds only
    if (buildId != null) {
        return "+build.$buildId"
    }

    // No tracking information could be found about the build
    return "+local"
}
