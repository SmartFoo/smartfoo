
# SmartFoo

[![Maven Central](https://img.shields.io/maven-central/v/com.smartfoo/smartfoo-android-lib-core)](https://central.sonatype.com/artifact/com.smartfoo/smartfoo-android-lib-core)
[![CI](https://github.com/SmartFoo/smartfoo/actions/workflows/android.yml/badge.svg)](https://github.com/SmartFoo/smartfoo/actions/workflows/android.yml)
[![Publish](https://github.com/SmartFoo/smartfoo/actions/workflows/publish.yml/badge.svg)](https://github.com/SmartFoo/smartfoo/actions/workflows/publish.yml)
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

The Android artifact is organized around small packages that wrap common
platform APIs behind SmartFoo-style helpers and callback managers:

| Package | Key Classes | Purpose |
|---|---|---|
| `core` | `FooString`, `FooListenerManager`, `FooListenerAutoStartManager`, `FooBoolean`, `FooObjects`, `FooArrays`, `FooRandom`, `FooBitSet`, `FooException`, `FooAssert`, `FooMemoryStream`, `FooReflection`, `FooTest` | Foundation utilities for string handling, listener management, common data-type helpers, assertions, in-memory byte I/O, reflection helpers, and test bootstrapping. |
| `annotations` | `NonNullNonEmpty` | AndroidX-based annotation for values that must be both non-null and non-empty. |
| `app` | `FooDebugActivity`, `FooDebugApplication`, `FooDebugConfiguration`, `CallbackFragment`, `CallbackDialogFragment`, `GenericPromptPositiveNegativeDialogFragment`, `GenericPromptSingleButtonDialogFragment` | App-level debug scaffolding and reusable fragment/dialog base classes that route UI events back through typed callbacks. |
| `bluetooth` | `FooBluetoothManager`, `FooBluetoothAdapterStateListener`, `FooBluetoothAudioConnectionListener`, `FooBluetoothUtils` | Classic Bluetooth and BLE capability checks, adapter state callbacks, Bluetooth audio connection monitoring, and adapter utility functions. |
| `bluetooth.gatt` | `FooGattManager`, `FooGattHandler`, `FooGattUtils`, `FooGattUuid`, `FooGattUuids`, `BluetoothGattCompat` | BLE GATT connection lifecycle management, per-device handlers, service discovery/read/write/notify state handling, UUID constants, and compatibility helpers. |
| `collections` | `FooCollections`, `FooBundleBuilder`, `FooLongSparseArray` | Collection equality/hash helpers, fluent Android `Bundle` construction, and a long-keyed sparse array implementation. |
| `content` | `FooPreferences` | `SharedPreferences` convenience wrapper with app/user preference files, typed accessors, and backup-change notifications. |
| `crypto` | `FooCrypto` | Hashing, HMAC-SHA-256, AES helper operations, secure-random bytes, and shared algorithm-name constants. |
| `logging` | `FooLog`, `FooLogPrinter`, `FooLogFormatter`, `FooLogAdbPrinter`, `FooLogFilePrinter`, `FooLogConsolePrinter`, `FooLogAndroidFormatter`, `FooLogUnixJavaFormatter`, `SetLogLimitDialogFragment` | Pluggable logging facade with ADB, file, and console printers, configurable formatters, and runtime log-size controls. |
| `media` | `FooAudioFocusController`, `FooAudioStreamVolumeObserver`, `FooAudioUtils`, `FooVolumeRestoringMediaPlayer`, `FooWiredHeadsetConnectionListener` | Audio-focus reference counting, stream-volume observation, audio attribute/stream helpers, volume-restoring playback, and wired headset events. |
| `network` | `FooDataConnectionManager`, `FooDataConnectionListener`, `FooCellularStateListener` | Connectivity and cellular call-state monitoring, including a combined data-availability manager that blocks use while disconnected or in a voice call. |
| `notification` | `FooNotification`, `FooNotificationBuilder`, `FooNotificationService`, `FooNotificationListenerManager`, `FooNotificationListener`, `FooNotificationReceiver` | Notification construction, notification listener service plumbing, posted/removed callback dispatch, and broadcast receiver integration. |
| `permission` | `FooPermission` | Individual runtime-permission checks plus battery-optimization exemption intents and launch helpers. |
| `permissions` | `FooPermissionsChecker` | Activity/fragment permission request orchestration with granted, denied, and rationale callbacks. |
| `platform` | `FooHandler`, `FooHandlerThread`, `FooService`, `FooBootListener`, `FooScreenListener`, `FooChargePortListener`, `FooPlatformUtils`, `FooRes` | Android primitive wrappers for handlers, services, boot/screen/charging broadcasts, platform inspection, and resource helpers. |
| `texttospeech` | `FooTextToSpeech`, `FooTextToSpeechBuilder`, `FooTextToSpeechHelper` | Singleton TTS engine wrapper with queued utterance sequencing, audio-focus integration, composite speech/silence/earcon builders, and convenience speak helpers. |
| `view` | `FooViewHolder`, `FooViewUtils` | ViewHolder caching for adapter views and logging/debug helpers for Android view visibility constants. |

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

Maintainer publishing instructions are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Requirements

- Android minSdk **34** (Android 14)
- Java **21** / Kotlin **2.x**

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
