pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven ( url= "https://kotlin.bintray.com/kotlinx" )
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven ( url= "https://kotlin.bintray.com/kotlinx" )

    }
}

rootProject.name = "RemoteAlarm"
include(":app")
 