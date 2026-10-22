import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.maxarcher.launcher"
    compileSdk = 37

    // Load local signing configuration without committing credentials.
    val keystoreFile = rootProject.file("keystore.properties")
    val signingProp = Properties()
    check(keystoreFile.isFile) {
        "Both debug and release require local signing configuration in keystore.properties."
    }
    keystoreFile.inputStream().use {
        signingProp.load(it)
    }
    val signingKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    signingKeys.forEach { key ->
        check(!signingProp.getProperty(key).isNullOrBlank()) {
            "Missing required signing property '$key' in keystore.properties."
        }
    }
    val signingStoreFile = rootProject.file(signingProp.getProperty("storeFile"))
    check(signingStoreFile.isFile) {
        "The keystore specified by storeFile in keystore.properties does not exist."
    }

    defaultConfig {
        applicationId = "com.maxarcher.launcher"
        minSdk = 31
        targetSdk = 36
        versionCode = 10
        versionName = "1.7.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = signingStoreFile
            storePassword = signingProp.getProperty("storePassword")
            keyAlias = signingProp.getProperty("keyAlias")
            keyPassword = signingProp.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        baseline = file("lint-baseline.xml")
        checkReleaseBuilds = true
        warningsAsErrors = true
        // Iteration 1 does not define a product icon; retain the platform fallback
        // instead of turning an invented placeholder into a product asset.
        disable += "MissingApplicationIcon"
        // Portrait-only presentation is the current product contract, so the locked
        // orientation on the single activity is intentional rather than a defect.
        disable += "LockedOrientationActivity"
        // Themed icons are out of scope. The launcher icon is an ordinary adaptive
        // icon, so the missing monochrome layer is intentional.
        disable += "MonochromeLauncherIcon"
        // Tool and dependency update notices are reviewed maintenance inputs, not
        // correctness failures for the currently selected platform baseline.
        disable += "AndroidGradlePluginVersion"
        disable += "GradleDependency"
        disable += "NewerVersionAvailable"
        disable += "OldTargetApi"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
