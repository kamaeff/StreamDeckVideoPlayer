plugins {
    kotlin("jvm") version "2.2.0"
}

group = "com.kamaeff.streamdeckvideo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.hid4java", "hid4java", "0.8.0")
    implementation("org.bytedeco:javacv-platform:1.5.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar.configure {
    manifest {
        attributes(mapOf("Main-Class" to "com.kamaeff.streamdeckvideo.MainKt"))
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

kotlin {
    jvmToolchain(23)
}