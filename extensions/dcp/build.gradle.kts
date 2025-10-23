plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.policy.engine.spi)
    implementation(libs.edc.request.policy.context.spi)
}
