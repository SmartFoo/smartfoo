
# SmartFoo

[![Maven Central](https://img.shields.io/maven-central/v/com.smartfoo/smartfoo-android-lib-core)](https://central.sonatype.com/artifact/com.smartfoo/smartfoo-android-lib-core)
[![CI](https://github.com/SmartFoo/smartfoo/actions/workflows/build.yml/badge.svg)](https://github.com/SmartFoo/smartfoo/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This project was started 2016/01 to [try to] be a pseudo cross-platform library
interface/implementation.

The primary philosophy of SmartFoo was/is to abstract out all direct platform
dependencies to the [developer] user.

Basically "Foo" annoyingly wraps literally **everything**.

Admittedly that does result in both:
* [A Smurf Naming Convention](https://blog.codinghorror.com/new-programming-jargon/).
* [A "15th" competing standard](https://xkcd.com/927/).

Foo/SmartFoo could be thought of as a bit analgous to the [Qt framework](https://doc.qt.io/qt-6/qt-intro.html),
which I did not know much about when I first started writing SmartFoo. The key
difference is that Qt is a single native C++ implementation that works on all
of its supported platforms. SmartFoo was intended to have separate language
implementations for each supported platform, but they would all have as
identical interfaces as possible as supported by the language & platform.

"FooString" would be the same for all supported platforms, but implemented in
each platform's preferred/default language.  
"FooLog" would be the same for all supported platforms...  
"FooNotification" would be the same for all supported platforms...  
...you get the point.

I came up with this [obvious not so brilliant probably horribly flawed] concept
while maintaining a code base that required me to bounce back and forth between:
* Windows Desktop Win32
* Windows CE Win32
* Windows Desktop .NET
* Windows CE .NET Compact Framework
* BlackBerry J2ME
* Android Java
* iOS ObjectiveC

The subtle differences between BlackBerry J2ME and Android Java were
eye-openers.  
Throw in some .NET Desktop and .NET Compact Framework differences and
similarities, and after about 4-5 platforms, plus Swift and Kotlin starting to
take off, and the core set of necessary classes started to become obvious [to
me].

I was able to successfully professionally maintain similar core code bases
across Win32, .NET, J2ME, Java, ... and almost ObjectiveC (thank god Swift came
around).

I was familiar with BOOST, SWIG, Qt, Xamarin, and others, but I personally
preferred native code and not generated or (literally .NET on mono) monolithic
code.

The original goal of SmartFoo was to initially implement a core set of
cross-platform classes in Android, iOS, .NET, and Win32, starting with Android.

Eventually other platforms could be abstracted out (yes, possibly even Qt).

## What's Included

| Package | Key Classes | Purpose |
|---|---|---|
| core | `FooString`, `FooLog`, `FooListenerManager` | Core utilities, logging facade, listener pattern |
| bluetooth | `FooBluetoothManager` | Bluetooth adapter state management |
| bluetooth.gatt | `FooGattManager`, `FooGattHandler` | BLE GATT connection lifecycle |
| media | `FooAudioFocusController`, `FooTextToSpeech` | Audio focus, TTS, volume control |
| network | `FooDataConnectionManager` | Data connection state monitoring |
| notification | `FooNotificationBuilder`, `FooNotificationService` | Notification construction and listening |
| platform | `FooHandler`, `FooHandlerThread`, `FooService` | Android platform primitives |

## Using the Library

Add the dependency to your Android project:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.smartfoo:smartfoo-android-lib-core:<version>")
}
```

The artifact is published to [Maven Central](https://central.sonatype.com/artifact/com.smartfoo/smartfoo-android-lib-core), which is included in Android projects by default. No extra repository configuration is needed.

See the [Releases](https://github.com/SmartFoo/smartfoo/releases) page for the changelog and version history.

## Requirements

- Android minSdk **34** (Android 14)
- Java **21** / Kotlin **2.x**

---

## Publishing a New Release

> This section is for maintainers only.

### One-time setup

> One-time setup instructions (Sonatype account, GPG key, Gradle properties, CI secrets) are documented in [CONTRIBUTING.md](CONTRIBUTING.md).

### Publishing

1. Choose the new version number. The version is derived automatically from the Git tag during CI publishing, so no file edits are required for a tag-triggered release. The fallback version in `android/smartfoo-android-lib-core/build.gradle.kts` can be kept in sync for reference.

2. Verify the publication locally (no credentials required, signing is skipped):
   ```bash
   cd android
   ./gradlew :smartfoo-android-lib-core:publishToMavenLocal
   ```

3. Publish to Maven Central:
   ```bash
   ./gradlew :smartfoo-android-lib-core:publishAllPublicationsToCentralPortal
   ```

4. Log in to [central.sonatype.com](https://central.sonatype.com) → Deployments, verify the artifacts, then click **Publish**. The release appears on Maven Central within ~15 minutes.

5. Tag the release:
   ```bash
   git tag v<version> && git push origin v<version>
   ```
   Pushing a `v*` tag also triggers the [publish GitHub Actions workflow](.github/workflows/publish.yml), which performs step 3 automatically using repository secrets (`MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `GPG_SIGNING_KEY`, `GPG_SIGNING_PASSWORD`).

---

## Frequently Unanswered Questions (FUQs)

### Why only Java (and a tiny bit of Kotlin)?

In 2016/01 Kotlin was barely a thing ([Kotlin v1.0 was released 2016/02/15](https://en.wikipedia.org/wiki/Kotlin_(programming_language)#Development)).
   
Kotlin quickly became interesting, and I use it in higher level apps, but for
lower level code I did not mind the OG Java and thought that sticking with it
might make transforming the code to other languages a little easier (there are
way more Java generators/parsers than Kotlin). True, Kotlin and Swift are more
alike than Java and Swift, but if I had to pick one lowest common denominator,
Java would be it.

I have used SmartFoo quite a bit in my personal Android projects, but I have
never expanded my needs for SmartFoo into other platforms (iOS, .NET, ...).

---

```
1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
         1         2         3         4         5         6         7         8         9         0
```