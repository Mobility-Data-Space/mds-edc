plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)
    implementation(libs.edc.core.lib)
}
