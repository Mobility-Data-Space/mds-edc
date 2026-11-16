plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.core.lib)
}
