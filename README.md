
# SmartFoo

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

1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
         1         2         3         4         5         6         7         8         9         0
