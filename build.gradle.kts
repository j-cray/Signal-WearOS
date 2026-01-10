// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register<Exec>("gitCommitAndPush") {
    workingDir = layout.projectDirectory.asFile
    commandLine("sh", "-c", "git add . && git commit -m 'Auto-commit: Update Signal WearOS app' && git push origin main")
    isIgnoreExitValue = true
}
