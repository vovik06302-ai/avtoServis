import java.security.KeyStore

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
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
      val rawKeystorePath = System.getenv("KEYSTORE_FILE")?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv("KEYSTORE_PATH")?.trim()?.takeIf { it.isNotEmpty() }
      val rawStorePassword = System.getenv("KEYSTORE_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv("STORE_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }
      val rawKeyAlias = System.getenv("KEY_ALIAS")?.trim()?.takeIf { it.isNotEmpty() }
        ?: if (System.getenv("KEYSTORE_PATH") != null) "upload" else null
      val rawKeyPassword = System.getenv("KEY_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }
        ?: rawStorePassword

      // Ищем файл keystore как по указанному пути (относительно app/), так и относительно корня проекта (rootDir)
      val resolvedKeystoreFile = rawKeystorePath?.let { path ->
        val direct = file(path)
        val fromRoot = rootProject.file(path)
        when {
          direct.exists() && direct.length() > 0L -> direct
          fromRoot.exists() && fromRoot.length() > 0L -> fromRoot
          else -> null
        }
      }

      var configuredWithReleaseKey = false
      if (resolvedKeystoreFile != null && !rawStorePassword.isNullOrBlank() && !rawKeyPassword.isNullOrBlank()) {
        try {
          val ks = KeyStore.getInstance(KeyStore.getDefaultType())
          resolvedKeystoreFile.inputStream().use { input ->
            ks.load(input, rawStorePassword.toCharArray())
          }
          val actualAlias = when {
            !rawKeyAlias.isNullOrBlank() && ks.containsAlias(rawKeyAlias) -> rawKeyAlias
            ks.aliases().hasMoreElements() -> ks.aliases().nextElement()
            else -> rawKeyAlias
          }
          if (!actualAlias.isNullOrBlank()) {
            storeFile = resolvedKeystoreFile
            storePassword = rawStorePassword
            keyAlias = actualAlias
            keyPassword = rawKeyPassword
            configuredWithReleaseKey = true
          }
        } catch (e: Exception) {
          logger.warn("Не удалось загрузить release keystore (${e.message}), используется резервная debug-подпись.")
        }
      }

      if (!configuredWithReleaseKey) {
        // Fallback на debug-подпись, если переменные окружения не заданы или keystore недоступен
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
  lint {
    checkReleaseBuilds = false
    abortOnError = false
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
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
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
}
