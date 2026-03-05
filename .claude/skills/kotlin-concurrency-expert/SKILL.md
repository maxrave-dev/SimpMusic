---
name: kotlin-concurrency-expert
description: Kotlin Coroutines review and remediation for Android. Use when asked to review concurrency usage, fix coroutine-related bugs, improve thread safety, or resolve lifecycle issues in Kotlin/Android code.
---

# Kotlin Concurrency Expert

## Overview

Review and fix Kotlin Coroutines issues in Android codebases by applying structured concurrency, lifecycle safety, proper scoping, and modern best practices with minimal behavior changes.

## Workflow

### 1. Triage the Issue

- Capture the exact error, crash, or symptom (ANR, memory leak, race condition, incorrect state).
- Check project coroutines setup: `kotlinx-coroutines-android` version, `lifecycle-runtime-ktx` version.
- Identify the current scope context (`viewModelScope`, `lifecycleScope`, custom scope, or none).
- Confirm whether the code is UI-bound (`Dispatchers.Main`) or intended to run off the main thread (`Dispatchers.IO`, `Dispatchers.Default`).
- Verify Dispatcher injection patterns for testability.

### 2. Apply the Smallest Safe Fix

Prefer edits that preserve existing behavior while satisfying structured concurrency and lifecycle safety.

Common fixes:

- **ANR / Main thread blocking**: Move heavy work to `withContext(Dispatchers.IO)` or `Dispatchers.Default`; ensure suspend functions are main-safe.
- **Memory leaks / zombie coroutines**: Replace `GlobalScope` with a lifecycle-bound scope (`viewModelScope`, `lifecycleScope`, or injected `applicationScope`).
- **Lifecycle collection issues**: Replace deprecated `launchWhenStarted` with `repeatOnLifecycle(Lifecycle.State.STARTED)`.
- **State exposure**: Encapsulate `MutableStateFlow` / `MutableSharedFlow`; expose read-only `StateFlow` or `Flow`.
- **CancellationException swallowing**: Ensure generic `catch (e: Exception)` blocks rethrow `CancellationException`.
- **Non-cooperative cancellation**: Add `ensureActive()` or `yield()` in tight loops for cooperative cancellation.
- **Callback APIs**: Convert listeners to `callbackFlow` with proper `awaitClose` cleanup.
- **Hardcoded Dispatchers**: Inject `CoroutineDispatcher` via constructor for testability.

## Critical Rules

### Dispatcher Injection (Testability)

```kotlin
// CORRECT: Inject dispatcher
class UserRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun fetchUser() = withContext(ioDispatcher) { ... }
}

// INCORRECT: Hardcoded dispatcher
class UserRepository {
    suspend fun fetchUser() = withContext(Dispatchers.IO) { ... }
}
```

### Lifecycle-Aware Collection

```kotlin
// CORRECT: Use repeatOnLifecycle
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> updateUI(state) }
    }
}

// INCORRECT: Direct collection (unsafe, deprecated)
lifecycleScope.launchWhenStarted {
    viewModel.uiState.collect { state -> updateUI(state) }
}
```

### State Encapsulation

```kotlin
// CORRECT: Expose read-only StateFlow
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}

// INCORRECT: Exposed mutable state
class MyViewModel : ViewModel() {
    val uiState = MutableStateFlow(UiState()) // Leaks mutability
}
```

### Exception Handling

```kotlin
// CORRECT: Rethrow CancellationException
try {
    doSuspendWork()
} catch (e: CancellationException) {
    throw e // Must rethrow!
} catch (e: Exception) {
    handleError(e)
}

// INCORRECT: Swallows cancellation
try {
    doSuspendWork()
} catch (e: Exception) {
    handleError(e) // CancellationException swallowed!
}
```

### Cooperative Cancellation

```kotlin
// CORRECT: Check for cancellation in tight loops
suspend fun processLargeList(items: List<Item>) {
    items.forEach { item ->
        ensureActive() // Check cancellation
        processItem(item)
    }
}

// INCORRECT: Non-cooperative (ignores cancellation)
suspend fun processLargeList(items: List<Item>) {
    items.forEach { item ->
        processItem(item) // Never checks cancellation
    }
}
```

### Callback Conversion

```kotlin
// CORRECT: callbackFlow with awaitClose
fun locationUpdates(): Flow<Location> = callbackFlow {
    val listener = LocationListener { location ->
        trySend(location)
    }
    locationManager.requestLocationUpdates(listener)
    
    awaitClose { locationManager.removeUpdates(listener) }
}
```

## Scope Guidelines

| Scope | Use When | Lifecycle |
|-------|----------|-----------|
| `viewModelScope` | ViewModel operations | Cleared with ViewModel |
| `lifecycleScope` | UI operations in Activity/Fragment | Destroyed with lifecycle owner |
| `repeatOnLifecycle` | Flow collection in UI | Started/Stopped with lifecycle state |
| `applicationScope` (injected) | App-wide background work | Application lifetime |
| `GlobalScope` | **NEVER USE** | Breaks structured concurrency |

## Testing Pattern

```kotlin
@Test
fun `loading data updates state`() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    val repository = FakeRepository()
    val viewModel = MyViewModel(repository, testDispatcher)
    
    viewModel.loadData()
    advanceUntilIdle()
    
    assertEquals(UiState.Success(data), viewModel.uiState.value)
}
```

## Reference Material

- [Kotlin Coroutines Best Practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [repeatOnLifecycle API](https://developer.android.com/topic/libraries/architecture/coroutines#repeatOnLifecycle)
