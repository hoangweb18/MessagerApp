import com.android.tools.r8.internal.im

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.messagerapp"
    compileSdk = 34

    packagingOptions {
       exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/INDEX.LIST")
    }

    defaultConfig {
        applicationId = "com.example.messagerapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.cast.framework)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // import
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    // implementation("com.google.firebase:firebase-auth:23.0.0")
     implementation("com.google.android.gms:play-services-auth:21.2.0")
     implementation("com.google.gms:google-services:4.3.4")
    // Scalable Size Unit ()
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    // Rounded ImageView
    implementation("com.makeramen:roundedimageview:2.3.0")


    //Multidex
    implementation("androidx.multidex:multidex:2.0.1")

    //Encrypt
    implementation("com.google.crypto.tink:tink-android:1.13.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")

    //auth
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")


    //SHA
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.android.play:integrity:1.3.0")

    implementation("com.google.firebase:firebase-storage:20.2.0")

    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("com.arthenica:mobile-ffmpeg-full-gpl:4.4.LTS")
    implementation("com.google.code.gson:gson:2.8.8")

    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")

}