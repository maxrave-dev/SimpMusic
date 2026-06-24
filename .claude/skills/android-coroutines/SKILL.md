---
name: android-coroutines
description: Authoritative rules and patterns for production-quality Kotlin Coroutines onto Android. Covers structured concurrency, lifecycle integration, and reactive streams.
---

# Android Coroutines Expert Skill

This skill provides authoritative rules and patterns for writing production-quality Kotlin Coroutines code on Android. It enforces structured concurrency, lifecycle safety, and modern best practices (2025 standards).

## Responsibilities

*   **Asynchronous Logic**: Implementing suspend functions, Dispatcher management, and parallel execution.
*   **Reactive Streams**: Implementing `Flow`, `StateFlow`, `SharedFlow`, and `callbackFlow`.
*   **Lifecycle Integration**: Managing scopes (`viewModelScope`, `lifecycleScope`) and safe collection (`repeatOnLifecycle`).
*   **Error Handling**: Implementing `CoroutineExceptionHandler`, `SupervisorJob`, and proper `try-catch` hierarchies.
*   **Cancellability**: Ensuring long-running operations are cooperative using `ensureActive()`.
*   **Testing**: Setting up `TestDispatcher` and `runTest`.

## Applicability

Activate this skill when the user asks to:
*   "Fetch data from an API/Database."
*   "Perform background processing."
*   "Fix a memory leak" related to threads/tasks.
*   "Convert a listener/callback to Coroutines."
*   "Implement a ViewModel."
*   "Handle UI state updates."

## Critical Rules & Constraints

### 1. Dispatcher Injection (Testability)
*   **NEVER** hardcode Dispatchers (e.g., `Dispatchers.IO`, `Dispatchers.Default`) inside classes.
*   **ALWAYS** inject a `CoroutineDispatcher` via the constructor.
*   **DEFAULT** to `Dispatchers.IO` in the constructor argument for convenience, but allow it to be overridden.

```kotlin
// CORRECT
class UserRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) { ... }

// INCORRECT
class UserRepository {
    fun getData() = withContext(Dispatchers.IO) { ... }
}
```

### 2. Main-Safety
*   All suspend functions defined in the Data or Domain layer must be **main-safe**.
*   **One-shot calls** should be exposed as `suspend` functions.
*   **Data changes** should be exposed as `Flow`.
*   The caller (ViewModel) should be able to call them from `Dispatchers.Main` without blocking the UI.
*   Use `withContext(dispatcher)` inside the repository implementation to move execution to the background.

### 3. Lifecycle-Aware Collection
*   **NEVER** collect a flow directly in `lifecycleScope.launch` or `launchWhenStarted` (deprecated/unsafe).
*   **ALWAYS** use `repeatOnLifecycle(Lifecycle.State.STARTED)` for collecting flows in Activities or Fragments.

```kotlin
// CORRECT
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { ... }
    }
}
```

### 4. ViewModel Scope Usage
*   Use `viewModelScope` for initiating coroutines in ViewModels.
*   Do not expose suspend functions from the ViewModel to the View. The ViewModel should expose `StateFlow` or `SharedFlow` that the View observes.

### 5. Mutable State Encapsulation
*   **NEVER** expose `MutableStateFlow` or `MutableSharedFlow` publicly.
*   Expose them as read-only `StateFlow` or `Flow` using `.asStateFlow()` or upcasting.

### 6. GlobalScope Prohibition
*   **NEVER** use `GlobalScope`. It breaks structured concurrency and leads to leaks.
*   If a task must survive the current scope, use an injected `applicationScope` (a custom scope tied to the Application lifecycle).

### 7. Exception Handling
*   **NEVER** catch `CancellationException` in a generic `catch (e: Exception)` block without rethrowing it.
*   Use `runCatching` only if you explicitly rethrow `CancellationException`.
*   Use `CoroutineExceptionHandler` only for top-level coroutines (inside `launch`). It has no effect inside `async` or child coroutines.

### 8. Cancellability
*   Coroutines feature **cooperative cancellation**. They don't stop immediately unless they check for cancellation.
*   **ALWAYS** call `ensureActive()` or `yield()` in tight loops (e.g., processing a large list, reading files) to check for cancellation.
*   Standard functions like `delay()` and `withContext()` are already cancellable.

### 9. Callback Conversion
*   Use `callbackFlow` to convert callback-based APIs to Flow.
*   **ALWAYS** use `awaitClose` at the end of the `callbackFlow` block to unregister listeners.

## Code Patterns

### Repository Pattern with Flow

```kotlin
class NewsRepository(
    private val remoteDataSource: NewsRemoteDataSource,
    private val externalScope: CoroutineScope, // For app-wide events
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val newsUpdates: Flow<List<News>> = flow {
        val news = remoteDataSource.fetchLatestNews()
        emit(news)
    }.flowOn(ioDispatcher) // Upstream executes on IO
}
```

### Parallel Execution

```kotlin
suspend fun loadDashboardData() = coroutineScope {
    val userDeferred = async { userRepo.getUser() }
    val feedDeferred = async { feedRepo.getFeed() }
    
    // Wait for both
    DashboardData(
        user = userDeferred.await(),
        feed = feedDeferred.await()
    )
}
```

### Testing with runTest

```kotlin
@Test
fun testViewModel() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    val viewModel = MyViewModel(testDispatcher)
    
    viewModel.loadData()
    advanceUntilIdle() // Process coroutines
    
    assertEquals(expectedState, viewModel.uiState.value)
}
```
