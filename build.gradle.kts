import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.21"
}

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("authercation") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

/**
 * Dump the declared methods of the deobfuscated
 * {@code ServerLoginPacketListenerImpl} from the resolved Minecraft runtime
 * so we can pick the correct mixin target method for the current MC version.
 *
 * Usage: ./gradlew dumpLoginListenerMethods
 *
 * This helper class lives in the main sourceset only for compilation convenience
 * and is excluded from the mod jar.
 */
tasks.register<JavaExec>("dumpLoginListenerMethods") {
	group = "dev"
	description = "Print ServerLoginPacketListenerImpl method names from the runtime classpath"

	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("net.auther.dev.PrintLoginListenerMethods")
	jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.named<Jar>("jar") {
	exclude("net/auther/dev/PrintLoginListenerMethods.class")
}


// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
