plugins {
    jacoco
}

dependencies {
    implementation(libs.edc.core.spi)
}

tasks.test {
    useJUnitPlatform()
}