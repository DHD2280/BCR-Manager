import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("dev.rikka.tools.materialthemebuilder")
}

val keystorePropertiesFile: File = rootProject.file("keystore/keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    signingConfigs {
        create("release") {
            storeFile = File(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    namespace = "it.dhd.bcrmanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "it.dhd.bcrmanager"
        minSdk = 28
        targetSdk = 34
        versionCode = 16
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", rootProject.name + "-v" + versionName + "-" + versionCode)
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            versionNameSuffix = ".debug"
        }
    }
    buildFeatures{
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    implementation("androidx.media3:media3-ui:1.3.0")
    val lifecycleVersion = "2.7.0"

    // AndroidX support
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Picasso
    implementation("com.squareup.picasso:picasso:2.8")


    // FastScroll
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")

    // Rx Java
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation ("io.reactivex.rxjava3:rxandroid:3.0.2")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel:$lifecycleVersion")

    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata:$lifecycleVersion")

    // Media 3
    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation ("androidx.media3:media3-common:1.3.0")
}