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
        mavenLocal()
    }
}

include(":extensions:policy:policy-always-true")
include(":extensions:policy:policy-referring-connector")
include(":extensions:policy:policy-time-interval")

include(":extensions:edp")

include(":launchers:connector-inmemory")
include(":launchers:connector-vault-postgresql")
include(":launchers:connector-vault-postgresql-edp")
include(":tests")
