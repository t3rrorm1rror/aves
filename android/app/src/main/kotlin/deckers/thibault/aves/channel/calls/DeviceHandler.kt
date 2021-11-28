package deckers.thibault.aves.channel.calls

import android.content.Context
import android.os.Build
import androidx.core.content.pm.ShortcutManagerCompat
import deckers.thibault.aves.channel.calls.Coresult.Companion.safe
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.util.*

class DeviceHandler(private val context: Context) : MethodCallHandler {
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getCapabilities" -> safe(call, result, ::getCapabilities)
            "getDefaultTimeZone" -> safe(call, result, ::getDefaultTimeZone)
            "getPerformanceClass" -> safe(call, result, ::getPerformanceClass)
            else -> result.notImplemented()
        }
    }

    private fun getCapabilities(@Suppress("unused_parameter") call: MethodCall, result: MethodChannel.Result) {
        val sdkInt = Build.VERSION.SDK_INT
        result.success(
            hashMapOf(
                "canGrantDirectoryAccess" to (sdkInt >= Build.VERSION_CODES.LOLLIPOP),
                "canPinShortcut" to ShortcutManagerCompat.isRequestPinShortcutSupported(context),
                "canPrint" to (sdkInt >= Build.VERSION_CODES.KITKAT),
                "canRenderEmojis" to (sdkInt >= Build.VERSION_CODES.LOLLIPOP),
                // as of google_maps_flutter v2.1.1, minSDK is 20 because of default PlatformView usage,
                // but using hybrid composition would make it usable on API 19 too,
                // cf https://github.com/flutter/flutter/issues/23728
                "canRenderGoogleMaps" to (sdkInt >= Build.VERSION_CODES.KITKAT_WATCH),
                "hasFilePicker" to (sdkInt >= Build.VERSION_CODES.KITKAT),
                "showPinShortcutFeedback" to (sdkInt >= Build.VERSION_CODES.O),
            )
        )
    }

    private fun getDefaultTimeZone(@Suppress("unused_parameter") call: MethodCall, result: MethodChannel.Result) {
        result.success(TimeZone.getDefault().id)
    }

    private fun getPerformanceClass(@Suppress("unused_parameter") call: MethodCall, result: MethodChannel.Result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val performanceClass = Build.VERSION.MEDIA_PERFORMANCE_CLASS
            if (performanceClass > 0) {
                result.success(performanceClass)
                return
            }
        }
        result.success(Build.VERSION.SDK_INT)
    }

    companion object {
        const val CHANNEL = "deckers.thibault/aves/device"
    }
}