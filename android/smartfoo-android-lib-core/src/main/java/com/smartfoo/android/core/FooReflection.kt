package com.smartfoo.android.core

import com.smartfoo.android.core.logging.FooLog
import kotlin.reflect.KClass

/**
 * Reflection and class-name utility functions for use in logging and diagnostics.
 *
 * Provides helpers for extracting short or fully-qualified class names from instances, [Class], or
 * [KClass] references, as well as inspecting inheritance hierarchies and reading field values.
 * Also supports mapping integer constants to their symbolic names via [mapConstants].
 */
@Suppress("unused")
object FooReflection {
    private val TAG = FooLog.TAG(FooReflection::class)

    /**
     * Returns the [Class] of [o], or null if [o] is null.
     *
     * @param o any object, or null
     * @return the runtime class, or null
     */
    @JvmStatic
    fun getClass(o: Any?) = o?.javaClass

    /**
     * Returns [c] unchanged. Provided for API uniformity.
     *
     * @param c a Java class
     * @return [c]
     */
    @JvmStatic
    fun getClass(c: Class<*>) = c

    /**
     * Returns the Java [Class] equivalent of the Kotlin [KClass].
     *
     * @param c a Kotlin class reference
     * @return the corresponding Java class
     */
    @JvmStatic
    fun getClass(c: KClass<*>) = c.java

    /**
     * Returns the full or short class name.
     * Overloads handle KClass, Class, and Any.
     */
    @JvmStatic
    @JvmOverloads
    fun getClassName(o: Any?, short: Boolean = true): String =
        getClassName(getClass(o)?.name, short)

    @JvmStatic
    @JvmOverloads
    fun getClassName(c: Class<*>?, short: Boolean = true): String =
        getClassName(c?.name, short)

    @JvmStatic
    @JvmOverloads
    fun getClassName(c: KClass<*>?, short: Boolean = true): String =
        getClassName(c?.java?.name, short)

    /**
     * Base logic for string manipulation
     */
    @JvmStatic
    fun getClassName(className: String?, shortClassName: Boolean): String {
        val name = className ?: return "null"
        return if (shortClassName) name.substringAfterLast('.') else name
    }

    /**
     * Convenience methods for "Short" names to satisfy Java callers
     * and maintain a clean API.
     */
    @JvmStatic
    fun getShortClassName(o: Any?): String = getClassName(o, true)

    @JvmStatic
    fun getShortClassName(c: Class<*>?): String = getClassName(c, true)

    @JvmStatic
    fun getShortClassName(c: KClass<*>?): String = getClassName(c, true)

    @JvmStatic
    fun getShortClassName(className: String?): String = getClassName(className, true)

    /**
     * Formats a method name for use in log output.
     *
     * Returns `"()"` if [methodName] is null, `"()"` if already equal to `"()"`, or
     * `".$methodName"` otherwise.
     *
     * @param methodName the method name to format, or null
     * @return formatted method name string
     */
    @JvmStatic
    fun getMethodName(methodName: String?): String {
        var methodName = methodName
        if (methodName == null) {
            methodName = "()"
        }
        if (methodName.compareTo("()") != 0) {
            methodName = ".$methodName"
        }
        return methodName
    }

    /**
     * Returns a combined short class name and formatted method name, e.g. `"MyClass.myMethod"`.
     *
     * @param o the object whose class name is used, or null
     * @param methodName the method name, or null to produce `"MyClass()"`
     * @return combined class-and-method string
     */
    @JvmStatic
    fun getShortClassAndMethodName(
        o: Any?,
        methodName: String?,
    ): String = getShortClassName(o) + getMethodName(methodName)

    /**
     * Returns a string describing the superclasses and interfaces implemented by [instance].
     *
     * Useful for debugging unknown object types at runtime.
     *
     * @param instance the object to inspect; must be non-null
     * @return a signature string such as `" extends Foo implements Bar"`
     */
    @JvmStatic
    fun <T> getInstanceSignature(instance: T): String {
        val sb = StringBuilder()
        val instanceClass = instance!!.javaClass
        getClassSignature(instanceClass, sb)
        getInterfaceSignature(instanceClass, sb)
        return sb
            .toString()
            // Remove any unspeakable/unprintable characters
            //noinspection TrimLambda
            .trim { it <= ' ' }
    }

    /**
     * Appends a `" extends ClassName, ..."` suffix to [sb] for each inner class of [instanceClass].
     *
     * @param instanceClass the class to inspect
     * @param sb            the builder to append to
     */
    @JvmStatic
    fun getClassSignature(
        instanceClass: Class<*>,
        sb: StringBuilder,
    ) {
        val instanceSubclasses = instanceClass.getClasses()
        if (instanceSubclasses.size > 0) {
            sb.append(" extends")
            for (i in instanceSubclasses.indices) {
                if (i > 0) {
                    sb.append(", ")
                }
                sb.append(' ').append(instanceSubclasses[i])
            }
        }
    }

    /**
     * Appends a `" implements InterfaceName, ..."` suffix to [sb] for each interface of
     * [instanceClass].
     *
     * @param instanceClass the class to inspect
     * @param sb            the builder to append to
     */
    @JvmStatic
    fun getInterfaceSignature(
        instanceClass: Class<*>,
        sb: StringBuilder,
    ) {
        val instanceInterfaces = instanceClass.getInterfaces()
        if (instanceInterfaces.size > 0) {
            sb.append(" implements")
            for (i in instanceInterfaces.indices) {
                if (i > 0) {
                    sb.append(", ")
                }
                sb.append(' ').append(instanceInterfaces[i])
            }
        }
    }

    /**
     * Returns true if [instanceActual] satisfies all subclass and interface constraints of
     * [instanceExpected].
     *
     * Both arguments may be null; returns false if either class cannot be resolved.
     *
     * @param instanceExpected the object whose class defines the expected type hierarchy
     * @param instanceActual the object to check against that hierarchy
     * @return true if [instanceActual] satisfies all type constraints of [instanceExpected]
     */
    @JvmStatic
    fun isAssignableFrom(
        instanceExpected: Any?,
        instanceActual: Any?,
    ): Boolean {
        val expectedInstanceClass = getClass(instanceExpected) ?: return false

        val actualInstanceClass = getClass(instanceActual) ?: return false

        //
        // Verify that actualInstanceClass is an instance of all subclasses and interfaces of expectedClass…
        //
        if (!expectedInstanceClass.isInterface) {
            val expectedSubclasses = expectedInstanceClass.getClasses()
            for (expectedSubclass in expectedSubclasses) {
                if (!expectedSubclass.isAssignableFrom(actualInstanceClass)) {
                    return false
                }
            }
        }

        val expectedInterfaces = expectedInstanceClass.getInterfaces()
        for (expectedInterface in expectedInterfaces) {
            if (!expectedInterface.isAssignableFrom(actualInstanceClass)) {
                return false
            }
        }

        return true
    }

    /**
     * Retrieves the value of a public field named [fieldName] from [o]'s class.
     *
     * Returns null if [o] is null, the field does not exist, or access is denied. Errors are
     * logged as warnings rather than propagated.
     *
     * @param o the object whose class is inspected; null returns null immediately
     * @param fieldName the exact name of the public field
     * @return the field value, or null on failure
     */
    @JvmStatic
    fun getFieldValue(
        o: Any?,
        fieldName: String,
    ): Any? {
        var fieldValue: Any? = null
        val c = getClass(o)
        if (c != null) {
            try {
                val field = c.getField(fieldName)
                try {
                    fieldValue = field.get(c)
                    //FooLog.v(TAG, "getFieldValue: fieldValue == " + fieldValue);
                } catch (e: IllegalAccessException) {
                    FooLog.w(TAG, "getFieldValue: get", e)
                }
            } catch (e: NoSuchFieldException) {
                FooLog.w(TAG, "getFieldValue: getField", e)
            }
        }
        return fieldValue
    }

    /**
     * Retrieves the value of a public [String] field named [fieldName] from [o]'s class.
     *
     * @param o the object whose class is inspected; null returns null
     * @param fieldName the exact name of the public field
     * @return the field value cast to [String], or null on failure or if the field value is null
     */
    @JvmStatic
    fun getFieldValueString(
        o: Any?,
        fieldName: String,
    ): String? = getFieldValue(o, fieldName) as String?



    /**
     * Kotlin-friendly overload of [mapConstants] that accepts a [KClass].
     *
     * @param clazz    the Kotlin class to inspect
     * @param prefixes constant name prefixes to match (e.g. `"REASON_"`)
     * @return a map from integer constant value to field name
     */
    @JvmStatic
    fun mapConstants(clazz: KClass<*>, vararg prefixes: String): Map<Int, String> =
        mapConstants(clazz.java, *prefixes)

    /***
     * Dynamically maps integer constant values to their field names using reflection.
     *
     * @param clazz The class to inspect (e.g., NotificationListenerService::class.java)
     * @param prefixes The constant prefix to look for (e.g., "REASON_")
     */
    @JvmStatic
    fun mapConstants(clazz: Class<*>, vararg prefixes: String): Map<Int, String> {
        return clazz.fields
            .filter { field ->
                prefixes.any { prefix -> field.name.startsWith(prefix) } &&
                        field.type == Int::class.javaPrimitiveType
            }
            .associate { field ->
                val value = runCatching { field.get(null) as Int }.getOrDefault(-1)
                value to field.name
            }
    }

    /**
     * Converts an integer [value] to its symbolic name using a constant map produced by
     * [mapConstants].
     *
     * @param map the integer-to-name map to look up
     * @param value the integer value to resolve
     * @param asFlags if true, treats [value] as a bitmask and joins all matching names with `"|"`;
     *   if false, looks up the exact value
     * @return a string such as `"REASON_FOO(3)"` or `"FLAG_A(1)|FLAG_B(2)"`
     */
    @JvmStatic
    fun toString(map: Map<Int, String>, value: Int, asFlags: Boolean = false): String {
        if (asFlags) {
            val joined = map.entries
                .filter { (key, _) -> (value and key) != 0 }
                .joinToString("|") { (key, name) -> "$name($key)" }
            return joined.ifEmpty { "0($value)" }
        } else {
            return (map[value] ?: "UNKNOWN").let { "$it($value)" }
        }
    }
}
