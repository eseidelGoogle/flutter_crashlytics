package com.kiwi.fluttercrashlytics

import android.content.Context
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


class FlutterCrashlyticsPlugin(context: Context) : MethodCallHandler {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_crashlytics")
            channel.setMethodCallHandler(FlutterCrashlyticsPlugin(registrar.activity()))
        }
    }

    init {
        Fabric.with(context, Crashlytics())
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val core = Crashlytics.getInstance().core
        when {
            call.method == "log" -> {
                if (call.arguments is String) {
                    core.log(call.arguments as String)
                } else {
                    val info = call.arguments as List<Any>
                    core.log(info[0] as Int, info[1] as String, info[2] as String)
                }
                result.success(null)
            }
            call.method == "setInfo" -> {
                val info = call.arguments as Map<String, Any>
                when (info["value"]) {
                    is String ->
                    core.setString(info["key"] as String, info["value"] as String)
                    is Int ->
                        core.setInt(info["key"] as String, info["value"] as Int)
                    is Double ->
                        core.setDouble(info["key"] as String, info["value"] as Double)
                    is  Boolean ->
                        core.setBool(info["key"] as String, info["value"] as Boolean)
                    is  Float ->
                        core.setFloat(info["key"] as String, info["value"] as Float)
                    is  Long ->
                        core.setLong(info["key"] as String, info["value"] as Long)
                    else -> core.log("ignoring unknown type with key ${info["key"]} and value ${info["value"]}")
                }
                result.success(null)
            }
            call.method == "setUserInfo" -> {
                val info = call.arguments as Map<String, String>
                core.setUserEmail(info["email"])
                core.setUserName(info["name"])
                core.setUserIdentifier(info["id"])
                result.success(null)
            }
            call.method == "reportCrash" -> {
                val exception = (call.arguments as Map<String, Any>)
                val cause = exception["cause"] as? String
                val message = exception["message"] as? String
                val traces = exception["trace"] as? List<List<Any>>
                val stack: MutableList<StackTraceElement> = mutableListOf()

                traces?.forEach {
                    stack.add(StackTraceElement(it[0] as? String ?: "", it[1] as? String
                            ?: "", it[2] as? String ?: "", it[3] as Int))
                }
                val throwable = FlutterException("$message\n$cause")
                throwable.stackTrace = stack.toTypedArray()

                core.logException(throwable)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}
