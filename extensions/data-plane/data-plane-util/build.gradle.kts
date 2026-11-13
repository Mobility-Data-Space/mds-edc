plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))

    implementation(libs.edc.core.lib)

    implementation(libs.opentelemetry.instrumentation.annotations)
}