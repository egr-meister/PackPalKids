import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Release signing: environment variables (CI) or an uncommitted signing.properties (local).
val signingProps = Properties().apply {
    val f = rootProject.file("signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: signingProps.getProperty(prop)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("ANDROID_KEYSTORE_PATH", "storeFile")
val releaseStorePassword = signingValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("ANDROID_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("ANDROID_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null } &&
    rootProject.file(releaseStoreFile!!).exists()

// Shrinking is opt-in (-PpackpalMinify=true) so a non-minified signed release is verified first.
val minifyRelease = (findProperty("packpalMinify") as String?)?.toBoolean() ?: false

android {
    namespace = "com.packpal.kids"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.packpal.kids"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = minifyRelease
            isShrinkResources = minifyRelease
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    dependenciesInfo {
        // No dependency metadata blob in APK/AAB: nothing is transmitted anyway, keep artifacts minimal.
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    jvmToolchain(17)
}

// Room Gradle plugin exports schemas per variant safely (parallel debug/release KSP runs
// sharing one schemaLocation can read each other's half-written JSON).
room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.generateKotlin", "true")
}

// Never fall back to debug signing: release packaging fails fast without real credentials.
gradle.taskGraph.whenReady {
    // Only tasks that produce release APK/AAB need the key; unit tests and lint for release do not.
    val signingTasks = setOf("assembleRelease", "bundleRelease", "packageRelease", "packageReleaseBundle", "signReleaseBundle")
    val wantsRelease = allTasks.any { t -> t.project == project && t.name in signingTasks }
    if (wantsRelease && !hasReleaseSigning) {
        throw GradleException(
            "Release signing credentials are missing. Set ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, " +
                "ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD (or create signing.properties). Debug builds do not need them."
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
