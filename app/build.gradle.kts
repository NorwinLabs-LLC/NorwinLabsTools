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

// 1. Versioning Logic: Read current values from version.properties
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()

if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
} else {
    versionProps["VERSION_CODE"] = "1"
    versionProps["VERSION_NAME"] = "1.0.0"
}

val verCode = versionProps["VERSION_CODE"].toString().toInt()
val verName = versionProps["VERSION_NAME"].toString()

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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // 2. Custom APK Naming: Renames the output file to include version info and build number
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val fileName = "NorwinLabsTools-v${variant.versionName}-b${variant.versionCode}-${variant.name}.apk"
            output.outputFileName = fileName
        }
    }
}

// 3. Increment Task: Updates the version.properties file for the NEXT build
tasks.register("incrementVersion") {
    group = "versioning"
    description = "Increments the version code and name in version.properties"
    
    val propsFile = versionPropsFile
    
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
        println("Version incremented to ${props.getProperty("VERSION_NAME")} (Code: $nextCode)")
    }
}

// 4. Create Build Info Task: Generates a text file with build details
tasks.register("createBuildInfo") {
    group = "build"
    description = "Generates a build-info.txt file in the releases folder"
    
    val releaseDirFile = rootProject.layout.projectDirectory.dir("releases").asFile
    val currentVerName = verName
    val currentVerCode = verCode
    
    doLast {
        if (!releaseDirFile.exists()) releaseDirFile.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val infoFile = File(releaseDirFile, "latest-build-info.txt")
        
        infoFile.writeText("""
            NorwinLabsTools Build Information
            --------------------------------
            Version Name: ${currentVerName}
            Build Number: ${currentVerCode}
            Build Date:   ${timestamp}
            Build Type:   Automated Internal Build
        """.trimIndent())
        
        println("Build info generated at: ${infoFile.absolutePath}")
    }
}

// 5. Copy Task: Moves built APKs to the top-level releases/ folder
tasks.register<Copy>("copyApkToReleases") {
    group = "build"
    description = "Copies the generated APKs to the project-level releases folder."
    
    from(layout.buildDirectory.dir("outputs/apk"))
    into(rootProject.layout.projectDirectory.dir("releases"))
    include("**/*.apk")
    includeEmptyDirs = false
    
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    // Flatten directory structure so APKs land directly in /releases
    eachFile {
        path = name
    }
    
    finalizedBy("createBuildInfo")
}

// 6. Automation Hook: Ensures incrementing happens before build and copying happens after
tasks.configureEach {
    if (name.startsWith("assemble")) {
        dependsOn("incrementVersion")
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