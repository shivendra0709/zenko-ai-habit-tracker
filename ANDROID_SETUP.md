# 📱 Zenko Android App - Setup Guide

**Build a native Android app that connects to the Zenko backend API.**

---

## 🛠️ Prerequisites

1. **Android Studio** (latest): https://developer.android.com/studio
2. **Java 21** or later
3. **Zenko Backend** running and deployed
4. **Google Play Services** (for Google Sign-In)

---

## 🚀 Quick Start

### Step 1: Create Android Project

1. Open Android Studio
2. Click File → New → New Project
3. Select "Empty Activity"
4. Project Name: `zenko-android`
5. Package: `com.zenko`
6. Minimum SDK: Android 8.0 (API 26)
7. Language: **Kotlin**
8. Click Finish

### Step 2: Update build.gradle.kts

**Project-level** `build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.0.0"
    kotlin("android") version "1.9.0"
}
```

**App-level** `build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = 34
    minSdk = 26

    defaultConfig {
        applicationId = "com.zenko"
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    // Kotlin
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Android UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.1")
    implementation("androidx.compose.material3:material3:1.0.1")
    implementation("androidx.compose.foundation:foundation:1.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    // Room Database (local caching)
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.1.1")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

---

## 📐 Architecture

### API Service (Retrofit)

Create `api/HabitService.kt`:
```kotlin
package com.zenko.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// Data Classes
data class Habit(
    val id: Long,
    val name: String,
    val emoji: String,
    val category: String,
    val frequency: String, // daily, weekly, monthly
    val color: String,
    val description: String?
)

data class HabitCompletion(
    val habitId: Long,
    val date: String, // YYYY-MM-DD
    val completed: Boolean
)

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val avatar: String?
)

// API Interface
interface HabitService {
    @GET("habits")
    suspend fun getHabits(): Response<List<Habit>>

    @POST("habits")
    suspend fun createHabit(@Body habit: Habit): Response<Habit>

    @PUT("habits/{id}")
    suspend fun updateHabit(@Path("id") id: Long, @Body habit: Habit): Response<Habit>

    @DELETE("habits/{id}")
    suspend fun deleteHabit(@Path("id") id: Long): Response<Unit>

    @POST("habits/{id}/complete")
    suspend fun markComplete(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("completions")
    suspend fun getCompletions(): Response<Map<String, Map<String, Boolean>>>

    @POST("auth/google")
    suspend fun googleSignIn(@Body body: Map<String, String>): Response<User>

    @POST("auth/logout")
    suspend fun logout(): Response<Map<String, String>>

    @GET("auth/user")
    suspend fun getCurrentUser(): Response<User>
}
```

---

### Retrofit Setup

Create `api/RetrofitClient.kt`:
```kotlin
package com.zenko.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = BuildConfig.API_URL
    
    private val httpClient: OkHttpClient
        get() {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            
            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    val habitService: HabitService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(HabitService::class.java)
    }
}
```

---

### ViewModel

Create `viewmodel/HabitViewModel.kt`:
```kotlin
package com.zenko.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenko.api.Habit
import com.zenko.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HabitViewModel : ViewModel() {
    private val habitService = RetrofitClient.habitService

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadHabits() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = habitService.getHabits()
                if (response.isSuccessful) {
                    _habits.value = response.body() ?: emptyList()
                    _error.value = null
                } else {
                    _error.value = "Failed to load habits"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun createHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                val response = habitService.createHabit(habit)
                if (response.isSuccessful) {
                    loadHabits()
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            try {
                habitService.deleteHabit(habitId)
                loadHabits()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun markComplete(habitId: Long, date: String) {
        viewModelScope.launch {
            try {
                habitService.markComplete(habitId, mapOf("date" to date))
                loadHabits()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
```

---

### UI with Jetpack Compose

Create `ui/HabitListScreen.kt`:
```kotlin
package com.zenko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenko.api.Habit
import com.zenko.viewmodel.HabitViewModel

@Composable
fun HabitListScreen(viewModel: HabitViewModel = viewModel()) {
    val habits by viewModel.habits.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHabits()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        Text(
            "🎯 Zenko - Habit Tracker",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(error ?: "Unknown error", modifier = Modifier.padding(16.dp))
                }
            }
            habits.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No habits yet. Add your first habit! ➕")
                }
            }
            else -> {
                LazyColumn {
                    items(habits) { habit ->
                        HabitCard(habit, onDelete = { viewModel.deleteHabit(habit.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(habit: Habit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(habit.emoji, style = MaterialTheme.typography.headlineSmall)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.bodyLarge)
                    Text(habit.category, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Text("✕")
                }
            }
        }
    }
}
```

---

### Main Activity

Create `MainActivity.kt`:
```kotlin
package com.zenko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.zenko.ui.HabitListScreen
import com.zenko.viewmodel.HabitViewModel

class MainActivity : ComponentActivity() {
    private val habitViewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HabitListScreen(habitViewModel)
        }
    }
}
```

---

## 📦 Build & Release

### Build APK
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (signed)
./gradlew assembleRelease
```

### Publish to Google Play
1. Create Google Play Developer account
2. Sign APK with release key
3. Upload to Google Play Console
4. Fill in store listing details
5. Wait for review (usually 2-4 hours)

---

## 🔗 Connect to Backend

In `app/build.gradle.kts`:
```kotlin
buildTypes {
    debug {
        buildConfigField("String", "API_URL", "\"http://10.0.2.2:8080/api/\"")
    }
    release {
        buildConfigField("String", "API_URL", "\"https://zenko-backend.onrender.com/api/\"")
    }
}
```

---

## 📱 Features for Android App

- ✅ View all habits
- ✅ Add/Edit/Delete habits
- ✅ Mark habits complete
- ✅ View streaks and stats
- ✅ Google Sign-In
- ✅ Offline viewing (with Room database)
- ✅ Push notifications (Firebase Cloud Messaging)
- 🔄 Real-time sync with backend

---

## 🧪 Test on Emulator

```bash
./gradlew emulatorDebug  # Build and run
```

---

## 📚 Resources

- Android Docs: https://developer.android.com/docs
- Retrofit: https://square.github.io/retrofit/
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Firebase: https://firebase.google.com/docs/android/setup

---

**Happy building! 🚀**
