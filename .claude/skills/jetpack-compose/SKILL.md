---
name: android-jetpack-compose
description: Use when building Android UIs with Jetpack Compose, managing state with remember/mutableStateOf, or implementing declarative UI patterns.
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
---

# Android - Jetpack Compose

Modern declarative UI toolkit for building native Android interfaces.

## Key Concepts

### State Management

Compose provides several ways to manage state:

- **remember**: Survives recomposition
- **rememberSaveable**: Survives configuration changes
- **mutableStateOf**: Creates observable state
- **derivedStateOf**: Computed state that updates when dependencies change

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }

    Column {
        Text("Count: $count")
        Button(onClick = { count++ }) {
            Text("Increment")
        }
    }
}

// With saveable for configuration changes
@Composable
fun SearchField() {
    var query by rememberSaveable { mutableStateOf("") }

    TextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search...") }
    )
}
```

### State Hoisting

Lift state up to make composables stateless and reusable:

```kotlin
// Stateless composable
@Composable
fun NameInput(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Name") },
        modifier = modifier
    )
}

// Stateful parent
@Composable
fun UserForm() {
    var name by remember { mutableStateOf("") }

    NameInput(
        name = name,
        onNameChange = { name = it }
    )
}
```

### ViewModel Integration

```kotlin
class UserViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun saveUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userRepository.save(_uiState.value.toUser())
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

@Composable
fun UserScreen(viewModel: UserViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UserContent(
        uiState = uiState,
        onNameChange = viewModel::updateName,
        onSave = viewModel::saveUser
    )
}
```

## Best Practices

### Composable Function Guidelines

```kotlin
// Use Modifier as first optional parameter
@Composable
fun CustomCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(16.dp)
        )
    }
}

// Use slot APIs for flexible content
@Composable
fun CustomScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = topBar,
        bottomBar = bottomBar,
        content = content
    )
}
```

### Efficient Recomposition

```kotlin
// Use keys for list items
@Composable
fun UserList(users: List<User>) {
    LazyColumn {
        items(
            items = users,
            key = { it.id }  // Stable key for efficient updates
        ) { user ->
            UserItem(user)
        }
    }
}

// Use derivedStateOf for expensive computations
@Composable
fun FilteredList(items: List<Item>, query: String) {
    val filteredItems by remember(items, query) {
        derivedStateOf {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    LazyColumn {
        items(filteredItems) { item ->
            ItemRow(item)
        }
    }
}
```

### Side Effects

```kotlin
// LaunchedEffect for coroutine-based side effects
@Composable
fun UserProfile(userId: String, viewModel: UserViewModel) {
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    // UI content
}

// DisposableEffect for cleanup
@Composable
fun LifecycleAwareComponent(lifecycle: Lifecycle) {
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            // Handle lifecycle events
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

// SideEffect for non-suspend side effects
@Composable
fun AnalyticsScreen(screenName: String) {
    SideEffect {
        analytics.logScreenView(screenName)
    }
}
```

## Common Patterns

### Navigation with Navigation Compose

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToDetail = { id ->
                    navController.navigate("detail/$id")
                }
            )
        }
        composable(
            route = "detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            DetailScreen(itemId = itemId)
        }
    }
}
```

### Material 3 Theming

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> darkColorScheme(
            primary = Purple80,
            secondary = PurpleGrey80,
            tertiary = Pink80
        )
        else -> lightColorScheme(
            primary = Purple40,
            secondary = PurpleGrey40,
            tertiary = Pink40
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Using theme values
@Composable
fun ThemedCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = "Themed content",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### Lists and Grids

```kotlin
@Composable
fun ProductGrid(products: List<Product>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductCard(product)
        }
    }
}

// Sticky headers
@Composable
fun ContactList(contacts: Map<Char, List<Contact>>) {
    LazyColumn {
        contacts.forEach { (initial, contactsForInitial) ->
            stickyHeader {
                Text(
                    text = initial.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(contactsForInitial) { contact ->
                ContactItem(contact)
            }
        }
    }
}
```

## Anti-Patterns

### Avoid Side Effects in Composition

Bad:

```kotlin
@Composable
fun BadExample(viewModel: ViewModel) {
    viewModel.loadData()  // Called on every recomposition!

    Text("Data loaded")
}
```

Good:

```kotlin
@Composable
fun GoodExample(viewModel: ViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Text("Data loaded")
}
```

### Don't Read State in Remember Block

Bad:

```kotlin
@Composable
fun BadCounter(initial: Int) {
    // Won't update when initial changes
    var count by remember { mutableStateOf(initial) }
}
```

Good:

```kotlin
@Composable
fun GoodCounter(initial: Int) {
    var count by remember(initial) { mutableStateOf(initial) }
}
```

### Avoid Heavy Computation During Composition

Bad:

```kotlin
@Composable
fun BadList(items: List<Item>) {
    // Runs on every recomposition
    val sorted = items.sortedBy { it.name }
    LazyColumn { /* ... */ }
}
```

Good:

```kotlin
@Composable
fun GoodList(items: List<Item>) {
    val sorted by remember(items) {
        derivedStateOf { items.sortedBy { it.name } }
    }
    LazyColumn { /* ... */ }
}
```

## Related Skills

- **android-architecture**: MVVM and clean architecture patterns
- **android-kotlin-coroutines**: Async operations in Compose
