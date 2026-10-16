plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.giraydecide.wear"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.giraydecide.wear"
        minSdk = 30
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 11
        versionName = "1.7.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false } }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
providers.gradleProperty("giraBuildRoot").orNull?.let { layout.buildDirectory.set(file("$it/app")) }
