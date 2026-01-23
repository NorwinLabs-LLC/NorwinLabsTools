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

// Helper to read version properties safely
fun getVersionProperty(key: String): String {
    val props = Properties()
    val propsFile = rootProject.file("version.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { props.load(it) }
    }
    return props.getProperty(key, if (key == "VERSION_CODE") "1" else "1.0.0")
}

val verCode = getVersionProperty("VERSION_CODE").toInt()
val verName = getVersionProperty("VERSION_NAME")
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
        create("release") {
            val keystoreFile = file("debug.keystore")
            if (keystoreFile.exists()) {
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
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
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
            // Re-adding the timestamp to ensure every build is unique in /releases
            val fileName = "NorwinLabsTools-v${variant.versionName}-b${variant.versionCode}-${variant.name}-$buildTimestamp.apk"
            output.outputFileName = fileName
        }
    }
}

tasks.register("incrementVersion") {
    group = "versioning"
    val propsFile = rootProject.file("version.properties")
    
    doLast {
        val props = Properties()
        if (propsFile.exists()) {
            propsFile.inputStream().use { props.load(it) }
        }
        val currentCode = props.getProperty("VERSION_CODE", "1").toInt()
        val nextCode = currentCode + 1
        props.setProperty("VERSION_CODE", nextCode.toString())
        
        val currentName = props.getProperty("VERSION_NAME", "1.0.0")
        val parts = currentName.split(".").toMutableList()
        if (parts.isNotEmpty()) {
            val lastPart = parts.last().toIntOrNull() ?: 0
            parts[parts.size - 1] = (lastPart + 1).toString()
            val nextName = parts.joinToString(".")
            props.setProperty("VERSION_NAME", nextName)
        }
        propsFile.outputStream().use { props.store(it, "Auto-incremented build version") }
        println("Version Incremented to: $nextCode")
    }
}

tasks.register("createBuildInfo") {
    group = "build"
    val propsFile = rootProject.file("version.properties")
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
        // Task dependency fix: increment version before starting assembly
        dependsOn("incrementVersion")
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