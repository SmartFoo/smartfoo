# Package com.smartfoo.android.core.logging

Structured logging facade and pluggable printer architecture. `FooLog` is the central singleton that dispatches `v/d/i/w/e` log calls to a registered set of `FooLogPrinter` implementations. Built-in printers cover ADB logcat (`FooLogAdbPrinter`), file output (`FooLogFilePrinter`), console/stdout (`FooLogConsolePrinter`), and Unix-style Java formatters. `FooLogFormatter` and its variants control the text format for each printer. A `SetLogLimitDialogFragment` lets users cap the on-disk log size at runtime.
