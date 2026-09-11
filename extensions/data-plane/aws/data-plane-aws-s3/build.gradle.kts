plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    api(libs.edc.participant.context.single.spi)
    implementation(libs.edc.util.lib)
    implementation(project(":extensions:data-plane:data-plane-util"))
    implementation(project(":extensions:data-plane:aws:aws-s3-core"))

    testImplementation(project(":extensions:data-plane:data-plane-core"))
    testImplementation(libs.edc.junit)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(testFixtures(libs.edc.http.lib))
}


