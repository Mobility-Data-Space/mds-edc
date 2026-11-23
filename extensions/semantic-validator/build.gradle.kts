plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.core.lib)
    implementation(libs.json.schema.validator)

    testImplementation(libs.assertj)
    testImplementation(libs.edc.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
}

tasks.test {
    inputs.file(rootProject.file("docs/usage/mds_features/mds_vocabulary_semantics.md"))
    providers.gradleProperty("updateDocs").orNull?.let { systemProperty("updateDocs", it) }
}
