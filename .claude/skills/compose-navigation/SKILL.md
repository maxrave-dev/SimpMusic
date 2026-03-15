---
name: compose-navigation
description: Implement navigation in Jetpack Compose using Navigation Compose. Use when asked to set up navigation, pass arguments between screens, handle deep links, or structure multi-screen apps.
---

# Compose Navigation

## Overview

Implement type-safe navigation in Jetpack Compose applications using the Navigation Compose library. This skill covers NavHost setup, argument passing, deep links, nested graphs, adaptive navigation, and testing.

## Setup

Add the Navigation Compose dependency:

```kotlin
// build.gradle.kts
dependencies {
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
    // For type-safe navigation (recommended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

// Enable serialization plugin
plugins {
    kotlin("plugin.serialization") version "2.0.21"
}
```

---

## Core Concepts

### 1. Define Routes (Type-Safe)

Use `@Serializable` data classes/objects for type-safe routes:

```kotlin
import kotlinx.serialization.Serializable

// Simple screen (no arguments)
@Serializable
object Home

// Screen with required argument
@Serializable
data class Profile(val userId: String)

// Screen with optional argument
@Serializable
data class Settings(val section: String? = null)

// Screen with multiple arguments
@Serializable
data class ProductDetail(val productId: String, val showReviews: Boolean = false)
```

### 2. Create NavController

```kotlin
@Composable
fun MyApp() {
    val navController = rememberNavController()
    
    AppNavHost(navController = navController)
}
```

### 3. Create NavHost

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        composable<Home> {
            HomeScreen(
                onNavigateToProfile = { userId ->
                    navController.navigate(Profile(userId))
                }
            )
        }
        
        composable<Profile> { backStackEntry ->
            val profile: Profile = backStackEntry.toRoute()
            ProfileScreen(userId = profile.userId)
        }
        
        composable<Settings> { backStackEntry ->
            val settings: Settings = backStackEntry.toRoute()
            SettingsScreen(section = settings.section)
        }
    }
}
```

---

## Navigation Patterns

### Basic Navigation

```kotlin
// Navigate forward
navController.navigate(Profile(userId = "user123"))

// Navigate and pop current screen
navController.navigate(Home) {
    popUpTo<Home> { inclusive = true }
}

// Navigate back
navController.popBackStack()

// Navigate back to specific destination
navController.popBackStack<Home>(inclusive = false)
```

### Navigate with Options

```kotlin
navController.navigate(Profile(userId = "user123")) {
    // Pop up to destination (clear back stack)
    popUpTo<Home> {
        inclusive = false  // Keep Home in stack
        saveState = true   // Save state of popped screens
    }
    
    // Avoid multiple copies of same destination
    launchSingleTop = true
    
    // Restore state when navigating to this destination
    restoreState = true
}
```

### Bottom Navigation Pattern

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentDestination?.hasRoute<Home>() == true,
                    onClick = {
                        navController.navigate(Home) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                // Add more items...
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
```

---

## Argument Handling

### Retrieve Arguments in Composable

```kotlin
composable<Profile> { backStackEntry ->
    val profile: Profile = backStackEntry.toRoute()
    ProfileScreen(userId = profile.userId)
}
```

### Retrieve Arguments in ViewModel

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val profile: Profile = savedStateHandle.toRoute<Profile>()
    
    val user: StateFlow<User?> = userRepository
        .getUser(profile.userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
```

### Complex Data: Pass IDs, Not Objects

```kotlin
// CORRECT: Pass only the ID
navController.navigate(Profile(userId = "user123"))

// In ViewModel, fetch from repository
class ProfileViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val profile = savedStateHandle.toRoute<Profile>()
    val user = userRepository.getUser(profile.userId)
}

// INCORRECT: Don't pass complex objects
// navController.navigate(Profile(user = complexUserObject)) // BAD!
```

---

## Deep Links

### Define Deep Links

```kotlin
@Serializable
data class Profile(val userId: String)

composable<Profile>(
    deepLinks = listOf(
        navDeepLink<Profile>(basePath = "https://example.com/profile")
    )
) { backStackEntry ->
    val profile: Profile = backStackEntry.toRoute()
    ProfileScreen(userId = profile.userId)
}
```

### Manifest Configuration

```xml
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="example.com" />
    </intent-filter>
</activity>
```

### Create PendingIntent for Notifications

```kotlin
val context = LocalContext.current
val deepLinkIntent = Intent(
    Intent.ACTION_VIEW,
    "https://example.com/profile/user123".toUri(),
    context,
    MainActivity::class.java
)

val pendingIntent = TaskStackBuilder.create(context).run {
    addNextIntentWithParentStack(deepLinkIntent)
    getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
```

---

## Nested Navigation

### Create Nested Graph

```kotlin
NavHost(navController = navController, startDestination = Home) {
    composable<Home> { HomeScreen() }
    
    // Nested graph for authentication flow
    navigation<AuthGraph>(startDestination = Login) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Home) {
                        popUpTo<AuthGraph> { inclusive = true }
                    }
                }
            )
        }
        composable<Register> { RegisterScreen() }
        composable<ForgotPassword> { ForgotPasswordScreen() }
    }
}

// Route definitions
@Serializable object AuthGraph
@Serializable object Login
@Serializable object Register
@Serializable object ForgotPassword
```

---

## Adaptive Navigation

Use `NavigationSuiteScaffold` for responsive navigation (bottom bar on phones, rail on tablets):

```kotlin
@Composable
fun AdaptiveApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = currentDestination?.hasRoute<Home>() == true,
                onClick = { navController.navigate(Home) }
            )
            item(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = currentDestination?.hasRoute<Settings>() == true,
                onClick = { navController.navigate(Settings()) }
            )
        }
    ) {
        AppNavHost(navController = navController)
    }
}
```

---

## Testing

### Setup

```kotlin
// build.gradle.kts
androidTestImplementation("androidx.navigation:navigation-testing:2.8.5")
```

### Test Navigation

```kotlin
class NavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var navController: TestNavHostController
    
    @Before
    fun setup() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            AppNavHost(navController = navController)
        }
    }
    
    @Test
    fun verifyStartDestination() {
        composeTestRule
            .onNodeWithText("Welcome")
            .assertIsDisplayed()
    }
    
    @Test
    fun navigateToProfile_displaysProfileScreen() {
        composeTestRule
            .onNodeWithText("View Profile")
            .performClick()
        
        assertTrue(
            navController.currentBackStackEntry?.destination?.hasRoute<Profile>() == true
        )
    }
}
```

---

## Critical Rules

### DO

- Use `@Serializable` routes for type safety
- Pass only IDs/primitives as arguments
- Use `popUpTo` with `launchSingleTop` for bottom navigation
- Extract `NavHost` to a separate composable for testability
- Use `SavedStateHandle.toRoute<T>()` in ViewModels

### DON'T

- Pass complex objects as navigation arguments
- Create `NavController` inside `NavHost`
- Navigate in `LaunchedEffect` without proper keys
- Forget `FLAG_IMMUTABLE` for PendingIntents (Android 12+)
- Use string-based routes (legacy pattern)

---

## References

- [Navigation with Compose](https://developer.android.com/develop/ui/compose/navigation)
- [Type-Safe Navigation](https://developer.android.com/guide/navigation/design#compose)
- [Pass Data Between Destinations](https://developer.android.com/guide/navigation/navigation-pass-data)
- [Test Navigation](https://developer.android.com/guide/navigation/navigation-testing)
