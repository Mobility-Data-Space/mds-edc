plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.decentralized.claims.spi)
    implementation(libs.edc.control.plane.spi)
}
