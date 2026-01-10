// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register<Exec>("setupGit") {
    workingDir = layout.projectDirectory.asFile
    commandLine("sh", "-c", "git init && git add . && git commit -m 'Initial commit' && git branch -M main")
    isIgnoreExitValue = true
}
