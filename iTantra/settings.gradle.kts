// Force Android preference location to a safe path without spaces
System.setProperty("ANDROID_USER_HOME", "C:\\Users\\Public\\.android")
// Clear other potential preference root properties to avoid conflicts
System.clearProperty("ANDROID_PREFS_ROOT")
System.clearProperty("android.user.home")
System.clearProperty("android.home")

pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "iTantraWiFi"
include(":app")