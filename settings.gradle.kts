pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Design tokens (com.derekwinters.chores:design-tokens) — GitHub Packages.
        // CI authenticates with the workflow GITHUB_TOKEN; for local builds set
        // gpr.user/gpr.key in ~/.gradle/gradle.properties (PAT with read:packages).
        maven {
            url = uri("https://maven.pkg.github.com/derekwinters/chores-web-design-tokens")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}

rootProject.name = "chores"
include(":app")
