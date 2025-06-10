import org.gradle.kotlin.dsl.ext

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
        mavenGpr("Mobility-Data-Space/mds-logging-house-client")
        mavenGpr("ids-basecamp/ids-infomodel-java")
        mavenLocal()
    }
}

fun RepositoryHandler.mavenGpr(project: String) {
    maven {
        setUrl("https://maven.pkg.github.com/$project")
        credentials {
            this.username = settings.ext.properties["gpr.user"] as String? ?: System.getenv("USERNAME")
            this.password = settings.ext.properties["gpr.key"] as String? ?: System.getenv("TOKEN")

            println("USERNAME is $username. password... can't tell :)")
        }
    }
}

include(":extensions:edp")
include(":extensions:manual-negotiation-approval")
include(":extensions:policy:policy-always-true")
include(":extensions:policy:policy-referring-connector")
include(":extensions:policy:policy-time-interval")
include(":extensions:semantic-validator")

include(":launchers:connector-inmemory")
include(":launchers:connector-vault-postgresql")
include(":launchers:connector-vault-postgresql-edp")
include(":tests")
