plugins {
    id("java")
}

group = "com.github.tanjagavric"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://pfsw.org/maven/repo/")
    }
}

dependencies {
    implementation("org.pfsw:pf-cda-core:2.8.0")
}
