plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.control.plane.spi)

    implementation(libs.edc.asset.api)
    implementation(libs.edc.core.lib)
    implementation(libs.edc.control.plane.transfer)
    implementation(libs.jakarta.annotation.api)

    testImplementation(libs.edc.junit)
}
