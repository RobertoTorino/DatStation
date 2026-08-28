// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}

tasks.register("buildAndOpen") {
    group = "custom"
    description = "Builds the APK and opens the output folder"
    dependsOn(":app:assembleDebug")
    doLast {
        val apkPath = "${project.rootDir}/app/build/outputs/apk/debug"
        println("Build Successful! Opening folder: $apkPath")
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            ProcessBuilder("cmd", "/c", "explorer", apkPath.replace("/", "\\")).start()
        } else if (os.contains("mac")) {
            ProcessBuilder("open", apkPath).start()
        }
    }
}

tasks.register("printDependencies") {
    group = "help"
    description = "Displays the dependency tree of the app module"
    dependsOn(":app:dependencies")
}

tasks.register("listAppTasks") {
    group = "help"
    description = "Lists all available tasks for the app module"
    dependsOn(":app:tasks")
}
