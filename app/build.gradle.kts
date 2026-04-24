import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
}

val externalPropertiesFile = "local.properties"
val keystoreProperties = Properties()

// Loads the keystore.properties file into the keystoreProperties object.
keystoreProperties.load(FileInputStream(externalPropertiesFile))

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(localFile.inputStream())
    }
}

android {
    namespace = "com.example.matchit"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.matchit"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MATCHING_SESSION_INVITE_CHANNEL_ID", "\"matching_session_invite_channel\"")
        buildConfigField("String", "FRIENDS_REQ_CHANNEL_ID", "\"friends_requests_channel\"")
        buildConfigField("String", "GENERAL_UPDATES_CHANNEL_ID", "\"general_updates_channel\"")

        manifestPlaceholders["googleMapsApiKey"] =
            localProperties.getProperty("GOOGLE_MAPS_API_KEY", "")
    }

    val localProperties = Properties().apply {
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            load(localFile.inputStream())
        }
    }
    val devServerIp = localProperties.getProperty("DEV_SERVER_IP", "10.0.2.2")

    flavorDimensions += "env"

    productFlavors {
        create("devEmu") {
            dimension = "env"
            buildConfigField("String", "ACCOUNTS_API_URL", "\"http://10.0.2.2:7050\"")
            buildConfigField("String", "FRIENDSHIPS_API_URL", "\"http://10.0.2.2:9050\"")
            buildConfigField("String", "DATA_API_URL", "\"http://10.0.2.2:8050\"")
            buildConfigField("String", "DATA_API_WEBSOCKET_URL", "\"ws://10.0.2.2:8050\"")
        }
        create("devDevice") {
            dimension = "env"
            buildConfigField("String", "ACCOUNTS_API_URL", "\"http://$devServerIp:7050/api/v1/\"")
            buildConfigField("String", "FRIENDSHIPS_API_URL", "\"http://$devServerIp:9050/api/v1/\"")
            buildConfigField("String", "DATA_API_URL", "\"http://$devServerIp:8050/api/v1/\"")
            buildConfigField("String", "DATA_API_WEBSOCKET_URL", "\"ws://$devServerIp:8050\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "ACCOUNTS_API_URL", "\"https://prod-user-account-service-370238596394.europe-west1.run.app/api/v1/\"")
            buildConfigField("String", "FRIENDSHIPS_API_URL", "\"https://prod-friendship-service-370238596394.europe-west1.run.app/api/v1/\"")
            buildConfigField("String", "DATA_API_URL", "\"https://prod-matching-sessions-service-370238596394.europe-west1.run.app/api/v1/\"")
            buildConfigField("String", "DATA_API_WEBSOCKET_URL", "\"wss://prod-matching-sessions-service-370238596394.europe-west1.run.app\"")
        }
        create("prod2") {
            dimension = "env"
            buildConfigField("String", "ACCOUNTS_API_URL", "\"http://app.matchit.cz:7050/api/v1/\"")
            buildConfigField("String", "FRIENDSHIPS_API_URL", "\"http://app.matchit.cz:9050/api/v1/\"")
            buildConfigField("String", "DATA_API_URL", "\"http://app.matchit.cz:8050/api/v1/\"")
            buildConfigField("String", "DATA_API_WEBSOCKET_URL", "\"ws://app.matchit.cz:8050\"")
        }
    }

    buildTypes {
        debug {

            //applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isMinifyEnabled = false

            // maximal number of participants in the matching session
            buildConfigField("String", "MATCHING_GROUP_MAX_SIZE", "\"3\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
       // additionalParameters = "-Xlint:deprecation"
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
     //   dataBinding = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    // Dagger - Hilt
        //implementation("com.google.dagger:hilt-android:2.49")
        implementation("androidx.test:core-ktx:1.5.0")
        implementation("com.google.android.gms:play-services-base:18.5.0")
        implementation("com.google.firebase:firebase-auth:23.0.0")
        implementation("androidx.fragment:fragment:1.8.6")
        implementation("com.google.android.gms:play-services-location:21.3.0")
        implementation("com.google.android.gms:play-services-maps:19.1.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation("androidx.compose.ui:ui-desktop:1.7.0")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")


    // retrofit
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // data store
        implementation("androidx.datastore:datastore-preferences:1.0.0")
        implementation("com.squareup.okhttp3:okhttp:4.12.0")


    // room
         val room_version = "2.6.1"

        // optional - Kotlin Extensions and Coroutines support for Room
        implementation("androidx.room:room-ktx:$room_version")
        implementation("androidx.room:room-runtime:$room_version")
        ksp("androidx.room:room-compiler:$room_version")

        // optional - RxJava2 support for Room
        /*implementation("androidx.room:room-rxjava2:$room_version")

        // optional - RxJava3 support for Room
        implementation("androidx.room:room-rxjava3:$room_version")*/

        // optional - Guava support for Room, including Optional and ListenableFuture
        implementation("androidx.room:room-guava:$room_version")

        // optional - Test helpers
        testImplementation("androidx.room:room-testing:$room_version")

        // optional - Paging 3 Integration
        implementation("androidx.room:room-paging:$room_version")


    // firebase
        implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
        implementation("com.google.firebase:firebase-messaging:24.0.0")

    // swiping cards
       implementation ("com.github.yuyakaido:CardStackView:v2.3.4")


    // credentials
        implementation("androidx.credentials:credentials:1.2.2")
        implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
        implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // other
        implementation("com.github.stfalcon-studio:StfalconImageViewer:1.0.1")
        implementation("androidx.core:core-ktx:1.9.0")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
        implementation("com.google.android.flexbox:flexbox:3.0.0")
        implementation("androidx.lifecycle:lifecycle-process:2.7.0")
        implementation("androidx.security:security-crypto:1.1.0-alpha06")

        // image cropping
        implementation("com.vanniktech:android-image-cropper:4.6.0")

        // youtube video player
        implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0")

        // fragment navigation

        val nav_version = "2.7.7"
        implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
        implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

        implementation("androidx.annotation:annotation:1.6.0")
        implementation("androidx.legacy:legacy-support-v4:1.0.0")
        implementation("com.github.bumptech.glide:glide:4.16.0")
        implementation("nl.dionsegijn:konfetti-xml:2.0.4")

        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        testImplementation("junit:junit:4.13.2")
        testImplementation("androidx.arch.core:core-testing:2.2.0")
        testImplementation("io.mockk:mockk:1.13.12")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

        //implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.8.0")
}