plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

tasks.register<Copy>("copyDebugApksToRoot") {
    dependsOn(":app:assembleV1Debug", ":app:assembleV2Debug")
    from(layout.projectDirectory.file("app/build/outputs/apk/v1/debug/app-v1-debug.apk"))
    from(layout.projectDirectory.file("app/build/outputs/apk/v2/debug/app-v2-debug.apk"))
    into(layout.projectDirectory)
}

tasks.register("assembleAndCopyDebugApks") {
    dependsOn("copyDebugApksToRoot")
}
