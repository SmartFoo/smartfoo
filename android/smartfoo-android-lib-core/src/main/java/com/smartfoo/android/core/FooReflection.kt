package com.smartfoo.android.core

import com.smartfoo.android.core.logging.FooLog
import kotlin.reflect.KClass

@Suppress("unused")
object FooReflection {
    private val TAG = FooLog.TAG(FooReflection::class)

    @JvmStatic
    fun getClass(o: Any?) = o?.javaClass

    @JvmStatic
    fun getClass(c: Class<*>) = c

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

    @JvmStatic
    fun getShortClassAndMethodName(
        o: Any?,
        methodName: String?,
    ): String = getShortClassName(o) + getMethodName(methodName)

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

    @JvmStatic
    fun getFieldValueString(
        o: Any?,
        fieldName: String,
    ): String? = getFieldValue(o, fieldName) as String?



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
