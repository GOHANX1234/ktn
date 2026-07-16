package com.android.support

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.LinkedHashSet

class Preferences private constructor(context: Context, preferencesName: String? = null) {

    init {
        val name = preferencesName ?: context.packageName + "_preferences"
        sharedPreferences = context.applicationContext
            .getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    companion object {
        private var sharedPreferences: SharedPreferences? = null
        private var prefsInstance: Preferences? = null

        @JvmField var context: Context? = null
        @JvmField var loadPref: Boolean = false
        @JvmField var isExpanded: Boolean = false

        private const val LENGTH                = "_length"
        private const val DEFAULT_STRING_VALUE  = ""
        private const val DEFAULT_INT_VALUE     = 0
        private const val DEFAULT_DOUBLE_VALUE  = 0.0
        private const val DEFAULT_FLOAT_VALUE   = 0f
        private const val DEFAULT_LONG_VALUE    = 0L
        private const val DEFAULT_BOOLEAN_VALUE = false

        // ── Native bridge ─────────────────────────────────────────────────
        @JvmStatic external fun Changes(
            con: Context, fNum: Int, fName: String, i: Int, bool: Boolean, str: String?
        )

        @JvmStatic
        fun changeFeatureInt(featureName: String, featureNum: Int, value: Int) {
            with(context!!).writeInt(featureNum, value)
            Changes(context!!, featureNum, featureName, value, false, null)
        }

        @JvmStatic
        fun changeFeatureString(featureName: String, featureNum: Int, str: String) {
            with(context!!).writeString(featureNum, str)
            Changes(context!!, featureNum, featureName, 0, false, str)
        }

        @JvmStatic
        fun changeFeatureBool(featureName: String, featureNum: Int, bool: Boolean) {
            with(context!!).writeBoolean(featureNum, bool)
            Changes(context!!, featureNum, featureName, 0, bool, null)
        }

        @JvmStatic
        fun loadPrefInt(featureName: String, featureNum: Int): Int {
            if (loadPref) {
                val i = with(context!!).readInt(featureNum)
                Changes(context!!, featureNum, featureName, i, false, null)
                return i
            }
            return 0
        }

        @JvmStatic
        fun loadPrefBool(featureName: String, featureNum: Int, bDef: Boolean): Boolean {
            var result = bDef
            val bool = with(context!!).readBoolean(featureNum, bDef)
            if (featureNum == -1) loadPref   = bool
            if (featureNum == -3) isExpanded = bool
            if (loadPref || featureNum < 0) result = bool
            Changes(context!!, featureNum, featureName, 0, result, null)
            return result
        }

        @JvmStatic
        fun loadPrefString(featureName: String, featureNum: Int): String {
            if (loadPref || featureNum <= 0) {
                val str = with(context!!).readString(featureNum)
                Changes(context!!, featureNum, featureName, 0, false, str)
                return str
            }
            return ""
        }

        // ── Instance factory ──────────────────────────────────────────────
        @JvmStatic
        fun with(context: Context): Preferences {
            if (prefsInstance == null) prefsInstance = Preferences(context)
            return prefsInstance!!
        }

        @JvmStatic
        fun with(context: Context, forceInstantiation: Boolean): Preferences {
            if (forceInstantiation) prefsInstance = Preferences(context)
            return prefsInstance!!
        }

        @JvmStatic
        fun with(context: Context, preferencesName: String): Preferences {
            if (prefsInstance == null) prefsInstance = Preferences(context, preferencesName)
            return prefsInstance!!
        }

        @JvmStatic
        fun with(context: Context, preferencesName: String, forceInstantiation: Boolean): Preferences {
            if (forceInstantiation) prefsInstance = Preferences(context, preferencesName)
            return prefsInstance!!
        }
    }

    // ── String ────────────────────────────────────────────────────────────
    fun readString(what: String): String =
        sharedPreferences!!.getString(what, DEFAULT_STRING_VALUE) ?: DEFAULT_STRING_VALUE

    fun readString(what: Int): String = try {
        sharedPreferences!!.getString(what.toString(), DEFAULT_STRING_VALUE) ?: DEFAULT_STRING_VALUE
    } catch (_: ClassCastException) { "" }

    fun readString(what: String, default: String): String =
        sharedPreferences!!.getString(what, default) ?: default

    fun writeString(where: String, what: String) =
        sharedPreferences!!.edit().putString(where, what).apply()

    fun writeString(where: Int, what: String) =
        sharedPreferences!!.edit().putString(where.toString(), what).apply()

    // ── Int ───────────────────────────────────────────────────────────────
    fun readInt(what: String): Int =
        sharedPreferences!!.getInt(what, DEFAULT_INT_VALUE)

    fun readInt(what: Int): Int = try {
        sharedPreferences!!.getInt(what.toString(), DEFAULT_INT_VALUE)
    } catch (_: ClassCastException) { 0 }

    fun readInt(what: String, default: Int): Int =
        sharedPreferences!!.getInt(what, default)

    fun writeInt(where: String, what: Int) =
        sharedPreferences!!.edit().putInt(where, what).apply()

    fun writeInt(where: Int, what: Int) =
        sharedPreferences!!.edit().putInt(where.toString(), what).apply()

    // ── Double ────────────────────────────────────────────────────────────
    fun readDouble(what: String): Double =
        if (!contains(what)) DEFAULT_DOUBLE_VALUE
        else java.lang.Double.longBitsToDouble(readLong(what))

    fun readDouble(what: String, default: Double): Double =
        if (!contains(what)) default
        else java.lang.Double.longBitsToDouble(readLong(what))

    fun writeDouble(where: String, what: Double) =
        writeLong(where, java.lang.Double.doubleToRawLongBits(what))

    // ── Float ─────────────────────────────────────────────────────────────
    fun readFloat(what: String): Float =
        sharedPreferences!!.getFloat(what, DEFAULT_FLOAT_VALUE)

    fun readFloat(what: String, default: Float): Float =
        sharedPreferences!!.getFloat(what, default)

    fun writeFloat(where: String, what: Float) =
        sharedPreferences!!.edit().putFloat(where, what).apply()

    // ── Long ──────────────────────────────────────────────────────────────
    fun readLong(what: String): Long =
        sharedPreferences!!.getLong(what, DEFAULT_LONG_VALUE)

    fun readLong(what: String, default: Long): Long =
        sharedPreferences!!.getLong(what, default)

    fun writeLong(where: String, what: Long) =
        sharedPreferences!!.edit().putLong(where, what).apply()

    // ── Boolean ───────────────────────────────────────────────────────────
    fun readBoolean(what: String): Boolean =
        sharedPreferences!!.getBoolean(what, DEFAULT_BOOLEAN_VALUE)

    fun readBoolean(what: Int): Boolean =
        sharedPreferences!!.getBoolean(what.toString(), DEFAULT_BOOLEAN_VALUE)

    fun readBoolean(what: String, default: Boolean): Boolean =
        sharedPreferences!!.getBoolean(what, default)

    fun readBoolean(what: Int, default: Boolean): Boolean = try {
        sharedPreferences!!.getBoolean(what.toString(), default)
    } catch (_: ClassCastException) { default }

    fun writeBoolean(where: String, what: Boolean) =
        sharedPreferences!!.edit().putBoolean(where, what).apply()

    fun writeBoolean(where: Int, what: Boolean) =
        sharedPreferences!!.edit().putBoolean(where.toString(), what).apply()

    // ── String Set ────────────────────────────────────────────────────────
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun putStringSet(key: String, value: Set<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            sharedPreferences!!.edit().putStringSet(key, value).apply()
        } else {
            putOrderedStringSet(key, value)
        }
    }

    fun putOrderedStringSet(key: String, value: Set<String>) {
        var stringSetLength = 0
        if (sharedPreferences!!.contains(key + LENGTH)) {
            stringSetLength = readInt(key + LENGTH)
        }
        writeInt(key + LENGTH, value.size)
        var i = 0
        for (entry in value) {
            writeString("$key[$i]", entry)
            i++
        }
        while (i < stringSetLength) {
            remove("$key[$i]")
            i++
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun getStringSet(key: String, defValue: Set<String>): Set<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            sharedPreferences!!.getStringSet(key, defValue) ?: defValue
        else
            getOrderedStringSet(key, defValue)

    fun getOrderedStringSet(key: String, defValue: Set<String>): Set<String> {
        if (contains(key + LENGTH)) {
            val set = LinkedHashSet<String>()
            val len = readInt(key + LENGTH)
            if (len >= 0) repeat(len) { i -> set.add(readString("$key[$i]")) }
            return set
        }
        return defValue
    }

    // ── Utility ───────────────────────────────────────────────────────────
    fun remove(key: String) {
        if (contains(key + LENGTH)) {
            val len = readInt(key + LENGTH)
            if (len >= 0) {
                sharedPreferences!!.edit().remove(key + LENGTH).apply()
                repeat(len) { i -> sharedPreferences!!.edit().remove("$key[$i]").apply() }
            }
        }
        sharedPreferences!!.edit().remove(key).apply()
    }

    fun contains(key: String): Boolean = sharedPreferences!!.contains(key)

    fun clear() = sharedPreferences!!.edit().clear().apply()
}
