plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'com.google.gms.google-services'
    id 'com.google.firebase.crashlytics'
    id 'com.google.devtools.ksp'
    id 'androidx.navigation.safeargs.kotlin'
    id 'kotlin-parcelize'
}

android {
    namespace 'com.yourstore.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.yourstore.app"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary true
        }
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.debug
        }
        debug {
            applicationIdSuffix ".debug"
            debuggable true
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
                targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        viewBinding true
        dataBinding true
    }

    packagingOptions {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
        }
    }
}

dependencies {
    // Android core libraries
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
    implementation "androidx.activity:activity-ktx:1.8.1"
    implementation "androidx.fragment:fragment-ktx:1.6.2"

    // Material design
    implementation 'com.google.android.material:material:1.10.0'

    // Navigation component
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.5'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.5'

    // RecyclerView and CardView
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'

    // Room Database
    implementation 'androidx.room:room-runtime:2.6.0'
    implementation 'androidx.room:room-ktx:2.6.0'
    ksp 'androidx.room:room-compiler:2.6.0'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3'

    // KTX extensions
    implementation 'androidx.core:core-ktx:1.12.0'

    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.6.0')
    implementation 'com.google.firebase:firebase-analytics-ktx'
    implementation 'com.google.firebase:firebase-auth-ktx'
    implementation 'com.google.firebase:firebase-firestore-ktx'
    implementation 'com.google.firebase:firebase-storage-ktx'
    implementation 'com.google.firebase:firebase-messaging-ktx'
    implementation 'com.google.firebase:firebase-crashlytics-ktx'

    // Dependency Injection - Koin
    implementation 'io.insert-koin:koin-android:3.5.0'

    // Image loading - Glide
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    ksp 'com.github.bumptech.glide:ksp:4.16.0'

    // Offline Caching
    implementation 'androidx.datastore:datastore-preferences:1.0.0'

    // CSV/Excel parsing
    implementation 'com.opencsv:opencsv:5.8'
    implementation 'org.apache.poi:poi:5.2.4'
    implementation 'org.apache.poi:poi-ooxml:5.2.4'

    // Circle ImageView
    implementation 'de.hdodenhof:circleimageview:3.1.0'

    // Swipe refresh layout
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}