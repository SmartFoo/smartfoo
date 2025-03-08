package com.smartfoo.android.testapp

import android.app.Application
import com.smartfoo.android.core.app.FooDebugApplication
import com.smartfoo.android.core.app.FooDebugConfiguration
import com.smartfoo.android.core.logging.FooLogCat

class MainApplication : Application(), FooDebugApplication {
    override fun getFooDebugConfiguration(): FooDebugConfiguration {
        return object : FooDebugConfiguration {
            override fun getDebugLogLimitKb(defaultValue: Int): Int {
                //...
                return FooLogCat.EMAIL_MAX_BYTES_DEFAULT
            }

            override fun setDebugLogLimitKb(value: Int) {
                //...
            }

            override fun getDebugLogEmailLimitKb(defaultValue: Int): Int {
                //...
                return FooLogCat.EMAIL_MAX_BYTES_DEFAULT
            }

            override fun setDebugLogEmailLimitKb(value: Int) {
                //...
            }

            override fun isDebugEnabled(): Boolean {
                //...
                return true
            }

            override fun setDebugEnabled(value: Boolean): Boolean {
                //...
                return true
            }

            override fun getDebugToFileEnabled(): Boolean {
                //...
                return false
            }

            override fun setDebugToFileEnabled(enabled: Boolean) {
                //...
            }
        }
    }
}