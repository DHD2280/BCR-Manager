import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.rikka.materialthemebuilder)
}

android {

    namespace = "it.dhd.bcrmanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "it.dhd.bcrmanager"
        minSdk = 28
        targetSdk = 34
        versionCode = 20
        versionName = "1.2.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", rootProject.name + "-v" + versionName + "-" + versionCode)
        resourceConfigurations.addAll(listOf(
            "en",
            "it",
            "lv"
        ))
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    var releaseSigning = signingConfigs.getByName("debug")

    try {
        val keystoreProperties = Properties()
        FileInputStream(keystorePropertiesFile).use { inputStream ->
            keystoreProperties.load(inputStream)
        }

        releaseSigning = signingConfigs.create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    } catch (ignored: Exception) {
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = releaseSigning
        }
        getByName("debug") {
            versionNameSuffix = ".debug"
            signingConfig = releaseSigning
        }
    }
    buildFeatures{
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf(
            "Red" to "F44336",
            "Pink" to "E91E63",
            "Purple" to "9C27B0",
            "DeepPurple" to "673AB7",
            "Indigo" to "3F51B5",
            "Blue" to "2196F3",
            "LightBlue" to "03A9F4",
            "Cyan" to "00BCD4",
            "Teal" to "009688",
            "Green" to "4FAF50",
            "LightGreen" to "8BC3A4",
            "Lime" to "CDDC39",
            "Yellow" to "FFEB3B",
            "Amber" to "FFC107",
            "Orange" to "FF9800",
            "DeepOrange" to "FF5722",
            "Brown" to "795548",
            "BlueGrey" to "607D8F",
            "Sakura" to "FF9CA8"
        )) {
            create("Material$name") {
                lightThemeFormat = "ThemeOverlay.Light.%s"
                darkThemeFormat = "ThemeOverlay.Dark.%s"
                primaryColor = "#$color"
            }
        }
    }
    packageName = "it.dhd.bcrmanager"
    // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
    // rikka.material >= 2.0.0 provides such attributes
    generatePalette = false
}


dependencies {

    implementation(libs.androidx.media3.ui)

    // AndroidX support
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)

    // Material Design
    implementation(libs.material)

    // Gson
    implementation(libs.gson)

    // Picasso
    implementation(libs.picasso)


    // FastScroll
    implementation(libs.library)

    // Rx Java
    implementation(libs.rxjava)
    implementation (libs.rxandroid)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel)

    // LiveData
    implementation(libs.androidx.lifecycle.livedata)

    // Media 3
    implementation(libs.androidx.media3.exoplayer)
    implementation (libs.androidx.media3.common)
}