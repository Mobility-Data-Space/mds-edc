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

include(":extensions:agreements:retirement-evaluation-core")
include(":extensions:agreements:retirement-evaluation-api")
include(":extensions:agreements:retirement-evaluation-spi")
include(":extensions:agreements:retirement-evaluation-store-sql")
include(":extensions:database-schema-migration-connector")
include(":extensions:data-plane-public-api-v2")
include(":extensions:edp")
include(":extensions:logging-house-publisher")
include(":extensions:manual-negotiation-approval")
include(":extensions:daps:oauth2-daps")
include(":extensions:daps:oauth2-identity-service")
include(":extensions:policy:policy-always-true")
include(":extensions:policy:policy-referring-connector")
include(":extensions:policy:policy-time-interval")
include(":extensions:semantic-validator")

include(":extensions:kafka:data-plane-kafka")
include(":extensions:kafka:data-plane-kafka-spi")

include(":launchers:connector-inmemory")
include(":launchers:connector-tck")
include(":launchers:connector-vault-postgresql")
include(":launchers:connector-vault-postgresql-edp")
include(":launchers:launcher-base")
include(":tests")
