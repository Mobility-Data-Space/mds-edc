import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
}

val edcVersion = libs.versions.edc

allprojects {
    apply(plugin = "java")

    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter()
            }
        }
    }

    tasks.withType<Test> {
        testLogging {
            events(TestLogEvent.FAILED)
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    // needed for E2E tests
    tasks.register("printClasspath") {
        dependsOn(tasks.compileJava)
        doLast {
            println(sourceSets["main"].runtimeClasspath.asPath)
        }
    }

}
