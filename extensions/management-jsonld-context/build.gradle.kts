plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.json.ld.spi)
    implementation(libs.edc.management.api.lib)
}
