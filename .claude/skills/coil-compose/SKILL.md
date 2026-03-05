---
name: coil-compose
description: Expert guidance on using Coil for image loading in Jetpack Compose. Use this when asked about loading images from URLs, handling image states, or optimizing image performance in Compose.
---

# Coil for Jetpack Compose

## Instructions

When implementing image loading in Jetpack Compose, use **Coil** (Coroutines Image Loader). It is the recommended library for Compose due to its efficiency and seamless integration.

### 1. Primary Composable: `AsyncImage`
Use `AsyncImage` for most use cases. It handles size resolution automatically and supports standard `Image` parameters.

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    placeholder = painterResource(R.drawable.placeholder),
    error = painterResource(R.drawable.error),
    contentDescription = stringResource(R.string.description),
    contentScale = ContentScale.Crop,
    modifier = Modifier.clip(CircleShape)
)
```

### 2. Low-Level Control: `rememberAsyncImagePainter`
Use `rememberAsyncImagePainter` only when you need a `Painter` instead of a composable (e.g., for `Canvas` or `Icon`) or when you need to observe the loading state manually.

> [!WARNING]
> `rememberAsyncImagePainter` does not detect the size your image is loaded at on screen and always loads the image with its original dimensions by default. Use `AsyncImage` unless a `Painter` is strictly required.

```kotlin
val painter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .size(Size.ORIGINAL) // Explicitly define size if needed
        .build()
)
```

### 3. Slot API: `SubcomposeAsyncImage`
Use `SubcomposeAsyncImage` when you need a custom slot API for different states (Loading, Success, Error).

> [!CAUTION]
> Subcomposition is slower than regular composition. Avoid using `SubcomposeAsyncImage` in performance-critical areas like `LazyColumn` or `LazyRow`.

```kotlin
SubcomposeAsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
    loading = {
        CircularProgressIndicator()
    },
    error = {
        Icon(Icons.Default.Error, contentDescription = null)
    }
)
```

### 4. Performance & Best Practices
*   **Singleton ImageLoader**: Use a single `ImageLoader` instance for the entire app to share the disk/memory cache.
*   **Main-Safe**: Coil executes image requests on a background thread automatically.
*   **Crossfade**: Always enable `crossfade(true)` in `ImageRequest` for a smoother transition from placeholder to success.
*   **Sizing**: Ensure `contentScale` is set appropriately to avoid loading larger images than necessary.

### 5. Checklist for implementation
- [ ] Prefer `AsyncImage` over other variants.
- [ ] Always provide a meaningful `contentDescription` or set it to `null` for decorative images.
- [ ] Use `crossfade(true)` for better UX.
- [ ] Avoid `SubcomposeAsyncImage` in lists.
- [ ] Configure `ImageRequest` for specific needs like transformations (e.g., `CircleCropTransformation`).
