import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// 1. Versioning Logic: Increments the version for the CURRENT build
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}

var verCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
var verName = versionProps.getProperty("VERSION_NAME", "1.0.0")

// Detect if we are running a build task
val isBuilding = gradle.startParameter.taskNames.any { 
    it.contains("assemble") || it.contains("install") || it.contains("bundle") 
}

if (isBuilding) {
    verCode++
    val parts = verName.split(".").toMutableList()
    if (parts.isNotEmpty()) {
        val lastPart = parts.last().toIntOrNull() ?: 0
        parts[parts.size - 1] = (lastPart + 1).toString()
        verName = parts.joinToString(".")
    }
    
    // Save immediately so the APK and the file are in sync
    versionProps.setProperty("VERSION_CODE", verCode.toString())
    versionProps.setProperty("VERSION_NAME", verName)
    versionPropsFile.outputStream().use { versionProps.store(it, "Auto-incremented build version") }
}

val buildTimestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())

android {
    namespace = "com.example.norwinlabstools"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.norwinlabstools"
        minSdk = 24
        targetSdk = 35
        versionCode = verCode
        versionName = verName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Use the actual .jks file found in your project for consistent cross-PC/GitHub patching
        val keystoreFile = file("debug.keystore.jks")
        if (keystoreFile.exists()) {
            create("sharedConfig") {
                storeFile = keystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("sharedConfig")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.findByName("sharedConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val fileName = "NorwinLabsTools-v${variant.versionName}-b${variant.versionCode}-${variant.name}.apk"
            output.outputFileName = fileName
        }
    }
}

// Configuration Cache safe tasks
tasks.register("createBuildInfo") {
    group = "build"
    val propsFile = versionPropsFile
    val releaseDirFile = rootProject.layout.projectDirectory.dir("releases").asFile
    
    doLast {
        val props = Properties()
        if (propsFile.exists()) {
            propsFile.inputStream().use { props.load(it) }
        }
        val vName = props.getProperty("VERSION_NAME", "1.0.0")
        val vCode = props.getProperty("VERSION_CODE", "1")

        if (!releaseDirFile.exists()) releaseDirFile.mkdirs()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val infoFile = File(releaseDirFile, "latest-build-info.txt")
        infoFile.writeText("""
            NorwinLabsTools Build Information
            Version Name: $vName
            Build Number: $vCode
            Build Date:   $timestamp
        """.trimIndent())
    }
}

tasks.register<Copy>("copyApkToReleases") {
    group = "build"
    from(layout.buildDirectory.dir("outputs/apk"))
    into(rootProject.layout.projectDirectory.dir("releases"))
    include("**/*.apk")
    includeEmptyDirs = false
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    eachFile { path = name }
}

tasks.configureEach {
    if (name.startsWith("assemble")) {
        finalizedBy("createBuildInfo")
        finalizedBy("copyApkToReleases")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.okhttp)
    implementation(libs.glide)
    implementation(libs.generativeai)
    implementation(libs.androidx.biometric)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}