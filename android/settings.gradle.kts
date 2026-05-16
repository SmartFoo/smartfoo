pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradleup.nmcp.settings") version "1.5.0"
}

val mavenCentralUsername = providers.gradleProperty("mavenCentralUsername").getOrNull()
    ?: providers.environmentVariable("MAVEN_CENTRAL_USERNAME").getOrNull()
val mavenCentralPassword = providers.gradleProperty("mavenCentralPassword").getOrNull()
    ?: providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").getOrNull()

nmcpSettings {
    centralPortal {
        mavenCentralUsername?.let { username.set(it) }
        mavenCentralPassword?.let { password.set(it) }
        // USER_MANAGED lets you review the deployment in the Central Portal UI before publishing.
        // Switch to AUTOMATIC once you've confirmed the first upload looks correct.
        publishingType.set("USER_MANAGED")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":smartfoo-android-lib-core")
include(":smartfoo-android-testapp")
include(":audiofocusthief")

rootProject.name = "smartfoo-android"
