plugins {
    kotlin("jvm") version "2.0.10"
}

group = "com.shinonometn"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.serialization)
    implementation("org.apache.groovy:groovy:4.0.19")
    implementation("ch.qos.logback:logback-classic:1.5.8")

    // https://mvnrepository.com/artifact/org.apache.poi/poi
    implementation("org.apache.poi:poi:5.3.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

/*
* See: https://www.baeldung.com/gradle-fat-jar
*/
task<Jar>("fatJar") {
    manifest {
        attributes["Main-Class"] = "com.shinonometn.template.live.server.ServerMainKt"
    }

    archiveFileName.set("server-${version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}