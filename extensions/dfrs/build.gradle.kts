import java.net.HttpURLConnection
import java.net.URI

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.core.spi)
    api(libs.edc.control.plane.spi)
    api(libs.edc.data.plane.spi)
    api(libs.edc.http.spi)
    api(libs.edc.participant.context.single.spi)
    implementation(project(":extensions:agreements:retirement-evaluation-spi"))
    implementation(libs.edc.dsp.catalog.transform.lib)

    testImplementation(libs.edc.json.ld.lib)
    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.assertj)
    testImplementation(libs.json.schema.validator)
}

val observerSchemasDir = layout.buildDirectory.dir("observer-schemas")

val downloadObserverSchemas = tasks.register("downloadObserverSchemas") {
    group = "build"
    description = "Downloads JSON schemas from the mds-observer repository"

    val outputDir = observerSchemasDir.get().asFile
    val sentinelFile = outputDir.resolve(".downloaded")
    outputs.file(sentinelFile)

    doLast {
        val baseUrl = "https://mobility-data-space.github.io/mds-observer"
        val schemaPaths = listOf(
            "schemas/v1/event-envelope.json",
            "schemas/v1/events/ContractNegotiationFinalized.json",
            "schemas/v1/events/TransferProcessStarted.json",
            "schemas/v1/events/ContractAgreementRetired.json"
        )

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        schemaPaths.forEach { path ->
            val connection = URI.create("$baseUrl/$path").toURL().openConnection() as HttpURLConnection
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode} fetching $path" }
            val target = outputDir.resolve(path)
            target.parentFile.mkdirs()
            target.writeBytes(connection.inputStream.readBytes())
        }

        sentinelFile.writeText("downloaded")
    }
}

tasks.named<ProcessResources>("processTestResources") {
    dependsOn(downloadObserverSchemas)
    from(observerSchemasDir.map { it.dir("schemas/v1") }) {
        into("schemas/v1")
    }
}
