import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

val resolvedVersionName: String = providers.gradleProperty("versionName").orNull
  ?.trim()
  ?.removePrefix("v")
  ?.removePrefix("V")
  ?.takeIf { it.isNotEmpty() }
  ?: System.getenv("GITHUB_REF_NAME")
    ?.trim()
    ?.removePrefix("v")
    ?.removePrefix("V")
    ?.takeIf { it.matches(Regex("^\\d+\\.\\d+\\.\\d+.*$")) }
  ?: "0.0.5"

val resolvedVersionCode: Int = providers.gradleProperty("versionCode").orNull
  ?.trim()
  ?.toIntOrNull()
  ?: run {
    val parts = resolvedVersionName.substringBefore("-").split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val semverCode = major * 10000 + minor * 100 + patch
    if (semverCode >= 5) semverCode else 5
  }

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    // Постоянный applicationId — никогда не меняется, чтобы обновление ставилось поверх старой версии
    applicationId = "com.aistudio.autoservice.kxmpzq"
    minSdk = 24
    targetSdk = 36
    versionCode = resolvedVersionCode
    versionName = resolvedVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    create("release") {
      val keystoreFilePath = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
        ?: System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
      val keystorePasswordValue = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
        ?: System.getenv("STORE_PASSWORD")?.takeIf { it.isNotBlank() }
      val keyAliasValue = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
        ?: if (System.getenv("KEYSTORE_PATH") != null) "upload" else null
      val keyPasswordValue = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }

      val candidateKeystoreFile = keystoreFilePath?.let { file(it) }
      val hasReleaseSigningEnv = candidateKeystoreFile != null &&
        candidateKeystoreFile.exists() &&
        !keystorePasswordValue.isNullOrBlank() &&
        !keyAliasValue.isNullOrBlank() &&
        !keyPasswordValue.isNullOrBlank()

      if (hasReleaseSigningEnv && candidateKeystoreFile != null) {
        storeFile = candidateKeystoreFile
        storePassword = keystorePasswordValue
        keyAlias = keyAliasValue
        keyPassword = keyPasswordValue
      } else {
        // Fallback на debug-подпись при локальной сборке без переменных окружения, чтобы сборка не падала
        val localDebugKeystore = file("${rootDir}/debug.keystore")
        if (localDebugKeystore.exists()) {
          storeFile = localDebugKeystore
          storePassword = "android"
          keyAlias = "androiddebugkey"
          keyPassword = "android"
        } else {
          val defaultDebug = signingConfigs.getByName("debug")
          storeFile = defaultDebug.storeFile
          storePassword = defaultDebug.storePassword
          keyAlias = defaultDebug.keyAlias
          keyPassword = defaultDebug.keyPassword
        }
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

ksp {
  arg("room.generateKotlin", "true")
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
