// settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // Adicione o JitPack aqui
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // Adicione o JitPack aqui também
    }
}


rootProject.name = "Terrometro"
include(":app")
