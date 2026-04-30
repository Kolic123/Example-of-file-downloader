plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("net.bytebuddy:byte-buddy:1.14.12")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

application {
    mainClass.set("com.example.MainKt");
}

tasks.register<JavaExec>("runDownloader") {
    group = "application"
    description = "Runs downloader with parameters"

    mainClass.set("org.example.MainKt")

    classpath = sourceSets["main"].runtimeClasspath


    args = project.findProperty("args")?.toString()?.split(" ") ?: emptyList()
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}