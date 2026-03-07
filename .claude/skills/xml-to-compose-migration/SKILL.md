---
name: xml-to-compose-migration
description: Convert Android XML layouts to Jetpack Compose. Use when asked to migrate Views to Compose, convert XML to Composables, or modernize UI from View system to Compose.
---

# XML to Compose Migration

## Overview

Systematically convert Android XML layouts to idiomatic Jetpack Compose, preserving functionality while embracing Compose patterns. This skill covers layout mapping, state migration, and incremental adoption strategies.

## Workflow

### 1. Analyze the XML Layout

- Identify the root layout type (`ConstraintLayout`, `LinearLayout`, `FrameLayout`, etc.).
- List all View widgets and their key attributes.
- Map data binding expressions (`@{}`) or view binding references.
- Identify custom views that need special handling.
- Note any `include`, `merge`, or `ViewStub` usage.

### 2. Plan the Migration

- Decide: **Full rewrite** or **incremental migration** (using `ComposeView`/`AndroidView`).
- Identify state sources (ViewModel, LiveData, savedInstanceState).
- List reusable components to extract as separate Composables.
- Plan navigation integration if using Navigation component.

### 3. Convert Layouts

Apply the layout mapping table below to convert each View to its Compose equivalent.

### 4. Migrate State

- Convert `LiveData` observation to `StateFlow` collection or `observeAsState()`.
- Replace `findViewById` / ViewBinding with Compose state.
- Convert click listeners to lambda parameters.

### 5. Test and Verify

- Compare visual output between XML and Compose versions.
- Test accessibility (content descriptions, touch targets).
- Verify state preservation across configuration changes.

---

## Layout Mapping Reference

### Container Layouts

| XML Layout | Compose Equivalent | Notes |
|------------|-------------------|-------|
| `LinearLayout (vertical)` | `Column` | Use `Arrangement` and `Alignment` |
| `LinearLayout (horizontal)` | `Row` | Use `Arrangement` and `Alignment` |
| `FrameLayout` | `Box` | Children stack on top of each other |
| `ConstraintLayout` | `ConstraintLayout` (Compose) | Use `createRefs()` and `constrainAs` |
| `RelativeLayout` | `Box` or `ConstraintLayout` | Prefer Box for simple overlap |
| `ScrollView` | `Column` + `Modifier.verticalScroll()` | Or use `LazyColumn` for lists |
| `HorizontalScrollView` | `Row` + `Modifier.horizontalScroll()` | Or use `LazyRow` for lists |
| `RecyclerView` | `LazyColumn` / `LazyRow` / `LazyGrid` | Most common migration |
| `ViewPager2` | `HorizontalPager` | From accompanist or Compose Foundation |
| `CoordinatorLayout` | Custom + `Scaffold` | Use `TopAppBar` with scroll behavior |
| `NestedScrollView` | `Column` + `Modifier.verticalScroll()` | Prefer Lazy variants |

### Common Widgets

| XML Widget | Compose Equivalent | Notes |
|------------|-------------------|-------|
| `TextView` | `Text` | Use `style` → `TextStyle` |
| `EditText` | `TextField` / `OutlinedTextField` | Requires state hoisting |
| `Button` | `Button` | Use `onClick` lambda |
| `ImageView` | `Image` | Use `painterResource()` or Coil |
| `ImageButton` | `IconButton` | Use `Icon` inside |
| `CheckBox` | `Checkbox` | Requires `checked` + `onCheckedChange` |
| `RadioButton` | `RadioButton` | Use with `Row` for groups |
| `Switch` | `Switch` | Requires state hoisting |
| `ProgressBar (circular)` | `CircularProgressIndicator` | |
| `ProgressBar (horizontal)` | `LinearProgressIndicator` | |
| `SeekBar` | `Slider` | Requires state hoisting |
| `Spinner` | `DropdownMenu` + `ExposedDropdownMenuBox` | More complex pattern |
| `CardView` | `Card` | From Material 3 |
| `Toolbar` | `TopAppBar` | Use inside `Scaffold` |
| `BottomNavigationView` | `NavigationBar` | Material 3 |
| `FloatingActionButton` | `FloatingActionButton` | Use inside `Scaffold` |
| `Divider` | `HorizontalDivider` / `VerticalDivider` | |
| `Space` | `Spacer` | Use `Modifier.size()` |

### Attribute Mapping

| XML Attribute | Compose Modifier/Property |
|---------------|--------------------------|
| `android:layout_width="match_parent"` | `Modifier.fillMaxWidth()` |
| `android:layout_height="match_parent"` | `Modifier.fillMaxHeight()` |
| `android:layout_width="wrap_content"` | `Modifier.wrapContentWidth()` (usually implicit) |
| `android:layout_weight` | `Modifier.weight(1f)` |
| `android:padding` | `Modifier.padding()` |
| `android:layout_margin` | `Modifier.padding()` on parent, or use `Arrangement.spacedBy()` |
| `android:background` | `Modifier.background()` |
| `android:visibility="gone"` | Conditional composition (don't emit) |
| `android:visibility="invisible"` | `Modifier.alpha(0f)` (keeps space) |
| `android:clickable` | `Modifier.clickable { }` |
| `android:contentDescription` | `Modifier.semantics { contentDescription = "" }` |
| `android:elevation` | `Modifier.shadow()` or component's `elevation` param |
| `android:alpha` | `Modifier.alpha()` |
| `android:rotation` | `Modifier.rotate()` |
| `android:scaleX/Y` | `Modifier.scale()` |
| `android:gravity` | `Alignment` parameter or `Arrangement` |
| `android:layout_gravity` | `Modifier.align()` |

---

## Common Patterns

### LinearLayout with Weights

```xml
<!-- XML -->
<LinearLayout android:orientation="horizontal">
    <View android:layout_weight="1" />
    <View android:layout_weight="2" />
</LinearLayout>
```

```kotlin
// Compose
Row(modifier = Modifier.fillMaxWidth()) {
    Box(modifier = Modifier.weight(1f))
    Box(modifier = Modifier.weight(2f))
}
```

### RecyclerView to LazyColumn

```xml
<!-- XML -->
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
// Compose
LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(items, key = { it.id }) { item ->
        ItemRow(item = item, onClick = { onItemClick(item) })
    }
}
```

### EditText with Two-Way Binding

```xml
<!-- XML with Data Binding -->
<EditText
    android:text="@={viewModel.username}"
    android:hint="@string/username_hint" />
```

```kotlin
// Compose
val username by viewModel.username.collectAsState()

OutlinedTextField(
    value = username,
    onValueChange = { viewModel.updateUsername(it) },
    label = { Text(stringResource(R.string.username_hint)) },
    modifier = Modifier.fillMaxWidth()
)
```

### ConstraintLayout Migration

```xml
<!-- XML -->
<androidx.constraintlayout.widget.ConstraintLayout>
    <TextView
        android:id="@+id/title"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent" />
    <TextView
        android:id="@+id/subtitle"
        app:layout_constraintTop_toBottomOf="@id/title"
        app:layout_constraintStart_toStartOf="@id/title" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

```kotlin
// Compose
ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
    val (title, subtitle) = createRefs()
    
    Text(
        text = "Title",
        modifier = Modifier.constrainAs(title) {
            top.linkTo(parent.top)
            start.linkTo(parent.start)
        }
    )
    Text(
        text = "Subtitle", 
        modifier = Modifier.constrainAs(subtitle) {
            top.linkTo(title.bottom)
            start.linkTo(title.start)
        }
    )
}
```

### Include / Merge → Extract Composable

```xml
<!-- XML: layout_header.xml -->
<merge>
    <ImageView android:id="@+id/avatar" />
    <TextView android:id="@+id/name" />
</merge>

<!-- Usage -->
<include layout="@layout/layout_header" />
```

```kotlin
// Compose: Extract as a reusable Composable
@Composable
fun HeaderSection(
    avatarUrl: String,
    name: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        AsyncImage(model = avatarUrl, contentDescription = null)
        Text(text = name)
    }
}

// Usage
HeaderSection(avatarUrl = user.avatar, name = user.name)
```

---

## Incremental Migration (Interop)

### Embedding Compose in XML

```xml
<!-- In your XML layout -->
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/compose_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
// In Fragment/Activity
binding.composeView.setContent {
    MaterialTheme {
        MyComposable()
    }
}
```

### Embedding XML Views in Compose

```kotlin
// Use AndroidView for Views that don't have Compose equivalents
@Composable
fun MapViewComposable(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                // Initialize the view
            }
        },
        update = { mapView ->
            // Update the view when state changes
        },
        modifier = modifier
    )
}
```

---

## State Migration

### LiveData to Compose

```kotlin
// Before: Observing in Fragment
viewModel.uiState.observe(viewLifecycleOwner) { state ->
    binding.title.text = state.title
}

// After: Collecting in Compose
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Text(text = uiState.title)
}
```

### Click Listeners

```kotlin
// Before: XML + setOnClickListener
binding.submitButton.setOnClickListener {
    viewModel.submit()
}

// After: Lambda in Compose
Button(onClick = { viewModel.submit() }) {
    Text("Submit")
}
```

---

## Checklist

- [ ] All layouts converted (no `include` or `merge` left)
- [ ] State hoisted properly (no internal mutable state for user input)
- [ ] Click handlers converted to lambdas
- [ ] RecyclerView adapters removed (using LazyColumn/LazyRow)
- [ ] ViewBinding/DataBinding removed
- [ ] Navigation integrated (NavHost or interop)
- [ ] Theming applied (MaterialTheme)
- [ ] Accessibility preserved (content descriptions, touch targets)
- [ ] Preview annotations added for development
- [ ] Old XML files deleted

## References

- [Interoperability APIs](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis)
- [Migration Strategy](https://developer.android.com/develop/ui/compose/migrate/strategy)
- [Compose and Views side by side](https://developer.android.com/develop/ui/compose/migrate)
