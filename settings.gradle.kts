rootProject.name = "mds-connector"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        mavenGpr("Mobility-Data-Space/mds-logging-house-client")
        mavenGpr("ids-basecamp/ids-infomodel-java")
        mavenLocal()
    }
}

fun RepositoryHandler.mavenGpr(project: String) {
    maven {
        setUrl("https://maven.pkg.github.com/$project")
        credentials {
            username = settings.ext.properties["gpr.user"] as String? ?: System.getenv("USERNAME")
            password = settings.ext.properties["gpr.key"] as String? ?: System.getenv("TOKEN")
        }
    }
}

include(":extensions:agreements:retirement-evaluation-api")
include(":extensions:agreements:retirement-evaluation-core")
include(":extensions:agreements:retirement-evaluation-spi")
include(":extensions:agreements:retirement-evaluation-store-sql")
include(":extensions:database-schema-migration-connector")
include(":extensions:data-address-store-patch")
include(":extensions:data-plane")
include(":extensions:data-plane:aws:aws-spi")
include(":extensions:data-plane:aws:aws-s3-core")
include(":extensions:data-plane:aws:data-plane-aws-s3")
include(":extensions:data-plane:aws:validator-data-address-s3")
include(":extensions:data-plane:azure:azure-blob-core")
include(":extensions:data-plane:azure:data-plane-azure-storage")
include(":extensions:data-plane:data-plane-core")
include(":extensions:data-plane:data-plane-http")
include(":extensions:data-plane:data-plane-http-oauth2-core")
include(":extensions:data-plane:data-plane-iam")
include(":extensions:data-plane:data-plane-public-api-v2")
include(":extensions:data-plane:data-plane-spi")
include(":extensions:data-plane:data-plane-util")
include(":extensions:data-plane:kafka:data-plane-kafka")
include(":extensions:data-plane:kafka:data-plane-kafka-spi")
include(":extensions:data-plane:observer")
include(":extensions:data-plane:sql")
include(":extensions:data-plane:sql:accesstokendata-store-sql")
include(":extensions:data-plane:sql:data-plane-store-sql")
include(":extensions:data-plane:sql:observer-sql")
include(":extensions:logging-house-publisher")
include(":extensions:manual-negotiation-approval")
include(":extensions:daps:oauth2-daps")
include(":extensions:daps:oauth2-identity-service")
include(":extensions:database-schema-migration-connector")
include(":extensions:dcp")
include(":extensions:embedded-data-plane")
include(":extensions:identity-hub:database-schema-migration-wallet")
include(":extensions:identity-hub:super-user-seeder")
include(":extensions:logging-house-publisher")
include(":extensions:management-jsonld-context")
include(":extensions:manual-negotiation-approval")
include(":extensions:policy:policy-always-true")
include(":extensions:policy:policy-referring-connector")
include(":extensions:policy:policy-time-interval")
include(":extensions:semantic-validator")

include(":launchers:connector-inmemory")
include(":launchers:connector-inmemory-dcp")
include(":launchers:connector-tck")
include(":launchers:connector-vault-postgresql")
include(":launchers:connector-vault-postgresql-dcp")
include(":launchers:wallet")
include(":launchers:base-connector")
include(":tests")
