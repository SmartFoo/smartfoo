# Package com.smartfoo.android.core.view

Android view utilities. `FooViewHolder` implements the ViewHolder pattern for `ListView`/`RecyclerView` adapters, caching child-view references in the parent view's tag via a `SparseArray` to avoid repeated `findViewById` calls. `FooViewUtils` provides helper functions for converting view visibility constants (`VISIBLE`, `INVISIBLE`, `GONE`) to human-readable strings for logging and debugging.
