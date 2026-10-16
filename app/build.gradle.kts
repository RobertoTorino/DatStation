plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

base {
    // Name output files "datstation-*" instead of "app-*".
    archivesName.set("datstation")
}

// The release workflow derives these from the git tag (v1.2.3 -> 1.2.3 / 10203) and passes
// them as -Pdatstation.versionName / -Pdatstation.versionCode. Local builds use the defaults.
val appVersionName = (project.findProperty("datstation.versionName") as String?) ?: "1.0.2"
val appVersionCode = (project.findProperty("datstation.versionCode") as String?)?.toInt() ?: 3

// Release signing comes from the environment so no keystore or password lives in the repo.
val keystorePath: String? = System.getenv("DATSTATION_KEYSTORE")
val hasReleaseSigning = !keystorePath.isNullOrBlank()

android {
    namespace = "com.robertotorino.datstation"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.robertotorino.datstation"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("DATSTATION_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DATSTATION_KEY_ALIAS")
                keyPassword = System.getenv("DATSTATION_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enables code shrinking, obfuscation, and optimization
            isShrinkResources = true // Removes unused resources
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    lint {
        abortOnError = true
        // The webp art is deliberately density-independent; the placement warning adds no value.
        disable += "IconLocation"
    }
}

// The 63 MB game catalogue is not in git. Fail early with a clear message instead of
// shipping an APK that crashes on first launch.
val checkCatalogueAsset by tasks.registering {
    description = "Verifies that the game catalogue asset is present."
    // A local File keeps the task configuration-cache compatible.
    val catalogueAsset = layout.projectDirectory.file("src/main/assets/datstation.db").asFile
    doLast {
        if (!catalogueAsset.exists()) {
            throw GradleException(
                "Missing ${catalogueAsset.path}. Copy datstation.db into app/src/main/assets/ " +
                    "(see DEVELOPMENT.md, section 'Game database')."
            )
        }
    }
}
tasks.named("preBuild") { dependsOn(checkCatalogueAsset) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.sqlcipher.android)
    implementation(libs.sqlite.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
