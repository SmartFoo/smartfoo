# Contributor Guidelines for `smartfoo`

## Repository overview
- The codebase is primarily an Android multi-module project that lives under [`android/`](android/).
- Gradle settings currently include the following modules: `:smartfoo-android-lib-core`, `:smartfoo-android-testapp`, and `:audiofocusthief`. The core runtime code that most changes will touch is inside `android/smartfoo-android-lib-core`.
- The library is published under the group `com.smartfoo` with version metadata maintained directly in each module's `build.gradle.kts` file.
- There are no nested `AGENTS.md` files yet; if you add one in the future remember that nested files override these root-level directions.

## Workflow expectations
- Always use the Gradle wrapper (`./gradlew`) that is checked into the repo. Do not replace it with a system-level Gradle installation.
- Keep changes scoped to the module(s) that actually need them. Avoid refactors that cascade across modules unless absolutely required to support your change.
- Update the top-level `README.md` or relevant module-level documentation when you add, remove, or significantly change features or project structure.
- Preserve existing TODO comments (for example, the `TODO:(pv)` notes) and add new ones using the same format if you must defer follow-up work.

## Coding style

### Java and Kotlin source
- Follow the established SmartFoo formatting style: four-space indentation, no tabs, and opening braces placed on their own line.
- Keep constant blocks aligned when editing existing code. Many files intentionally align the `=` and comments for readability—match the surrounding style rather than reformatting whole sections.
- Group imports by package and keep AndroidX/Jetpack imports separate from `com.smartfoo` packages. Avoid wildcards.
- When adding new classes, mirror the existing package structure (for example `com.smartfoo.android.core.*`) and keep filenames identical to the public class/interface they contain.
- Annotate nullability using AndroidX annotations (`@NonNull`, `@Nullable`, etc.) where appropriate and consistent with nearby code.
- Prefer Java for low-level library features unless a module is already Kotlin-based. When Kotlin is used, follow the official Kotlin coding conventions and keep JVM target compatibility consistent with the module's Gradle configuration.

### Android resources
- Place shared resources in the `android/smartfoo-android-lib-core/src/main/res` tree; reserve module-specific assets for their own `res` directories.
- Resource names should be lowercase with underscores. Keep existing prefixes (for example `foo_`) where they are already used.
- Update `strings.xml` entries when text changes and include descriptive comments for non-obvious values.

## Testing and validation
- Run unit tests for any modules you modify. Typical commands include:
  - `cd android && ./gradlew :smartfoo-android-lib-core:test`
  - `cd android && ./gradlew :audiofocusthief:test`
- If you touch instrumentation code, run `./gradlew :<module>:connectedAndroidTest` when an emulator/device is available. Document in your PR description when emulator-based tests could not be executed.
- `./gradlew lint` is available for Android modules; execute it when you make UI or resource-heavy changes.
- Do not disable the existing debug coverage options (`enableUnitTestCoverage` / `enableAndroidTestCoverage`) unless there is a compelling reason.

## Versioning and compatibility
- The core library currently targets `compileSdk = 35` and `minSdk = 34`. Do not change these values without coordinating with maintainers and updating any compatibility notes.
- When adding new dependencies, declare them through `android/gradle/libs.versions.toml` so that versions stay centralized.

## Pull requests
- Keep commits focused and well-described. Include context on why the change is needed and highlight any follow-up work.
- Ensure every PR includes a summary of manual or automated testing performed.
