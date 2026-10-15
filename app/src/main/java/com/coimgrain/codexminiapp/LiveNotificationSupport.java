package com.coimgrain.codexminiapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Platform probing and decoration for the "live" notification surfaces users know as
 * 灵动岛 / 实况窗 / 流体云 / 胶囊.
 *
 * <p>Those surfaces are driven by AOSP's promotion rules (Android 16 "Rich Ongoing
 * Notifications"). A notification only qualifies when it is ongoing, carries a
 * non-empty title, is colorized, uses no custom RemoteViews, is not a group summary
 * and uses one of the promotable styles (null / BigTextStyle / CallStyle /
 * ProgressStyle). OPPO / OnePlus ColorOS 16 and later render exactly such a
 * notification as a 流体云 capsule, gated by a per-app system switch the user must
 * enable — no vendor SDK and no white-listing is involved.</p>
 *
 * <p>Everything version- and vendor-specific here is reflective and best-effort: a
 * missing class, a missing method or a disabled aconfig flag degrades to an ordinary
 * ongoing notification, never to a failure.</p>
 */
final class LiveNotificationSupport {
    private static final String TAG = "GPTMiniLiveNotify";

    /** Android 16 (API 36) introduced ProgressStyle and the promotion rules. */
    static final int API_ANDROID_16 = 36;

    private static final String CLASS_PROGRESS_STYLE = "android.app.Notification$ProgressStyle";
    private static final String EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing";
    private static final String EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText";
    /** Status-bar chips truncate hard; Android documents ~7 characters. */
    private static final int MAX_SHORT_TEXT_LENGTH = 7;

    private LiveNotificationSupport() {
    }

    /**
     * Applies everything this device understands to an ongoing "live" notification.
     *
     * <p>{@code running} gates the promotion requests on purpose: while a task runs we
     * want the capsule / chip, while the service is idle we deliberately stay a plain
     * ongoing notification so the status bar is not permanently occupied.</p>
     */
    static Notification.Builder decorate(
            Context context,
            Notification.Builder builder,
            boolean running,
            String shortCriticalText
    ) {
        if (builder == null) return null;
        if (running) {
            // Colorized is a hard prerequisite of the AOSP promotion rules.
            builder.setColorized(true);
            applyShortCriticalText(builder, shortCriticalText);
            requestPromotedOngoing(builder);
            applyAndroid16ProgressStyle(context, builder);
        }
        return builder;
    }

    /**
     * Android 16 progress-centric notifications. The style class only exists from API
     * 36 (and is behind the {@code android.app.api_rich_ongoing} flag) while this
     * project still compiles against 35, so it is built reflectively.
     */
    private static void applyAndroid16ProgressStyle(Context context, Notification.Builder builder) {
        if (Build.VERSION.SDK_INT < API_ANDROID_16 || context == null) return;
        Object progressStyle = newInstance(CLASS_PROGRESS_STYLE);
        if (progressStyle == null) return;
        invokeOneArg(progressStyle, "setStyledByProgress", Boolean.TRUE);
        try {
            invokeOneArg(
                    progressStyle,
                    "setProgressTrackerIcon",
                    Icon.createWithResource(context, R.drawable.ic_notification)
            );
        } catch (Throwable error) {
            Log.d(TAG, "progress tracker icon unavailable");
        }
        // Assigning a style also overrides the plain setProgress extras, which is the
        // documented behaviour for ProgressStyle.
        invokeOneArg(builder, "setStyle", progressStyle);
    }

    /**
     * Compact status-bar chip text: {@code Builder#setShortCriticalText} exists from
     * 36.1, on API 36 the same value travels as the {@code android.shortCriticalText}
     * extra. Unknown keys are kept in the extras bundle, so writing both is harmless.
     */
    private static void applyShortCriticalText(
            Notification.Builder builder,
            String shortCriticalText
    ) {
        String value = normalizeShortText(shortCriticalText);
        if (value.isEmpty()) return;
        if (!invokeOneArg(builder, "setShortCriticalText", value)) {
            addExtra(builder, EXTRA_SHORT_CRITICAL_TEXT, value);
        }
    }

    /**
     * Opts an ongoing notification into the promoted ("live update") surface.
     * The public setter only exists from 36.1; on API 36 the documented path is the
     * {@code android.requestPromotedOngoing} extra.
     */
    private static void requestPromotedOngoing(Notification.Builder builder) {
        addExtra(builder, EXTRA_REQUEST_PROMOTED_ONGOING, Boolean.TRUE);
        invokeOneArg(builder, "setRequestPromotedOngoing", Boolean.TRUE);
    }

    /**
     * {@code NotificationManager#canPostPromotedNotifications()} — API 36, flagged, so
     * reflective. False means the user or the ROM disabled promoted notifications.
     */
    static boolean canPostPromotedNotifications(Context context) {
        if (context == null || Build.VERSION.SDK_INT < API_ANDROID_16) return false;
        try {
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager == null) return false;
            Method method = NotificationManager.class.getMethod("canPostPromotedNotifications");
            Object result = method.invoke(manager);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable error) {
            Log.d(TAG, "canPostPromotedNotifications unavailable");
            return false;
        }
    }

    /**
     * Logs whether a built notification actually qualifies for promotion. Purely
     * diagnostic: it makes "no capsule appeared" explainable from logcat.
     */
    static void logPromotionState(Context context, Notification notification) {
        if (notification == null) return;
        boolean promotable = false;
        try {
            Method method = Notification.class.getMethod("hasPromotableCharacteristics");
            Object result = method.invoke(notification);
            promotable = result instanceof Boolean && (Boolean) result;
        } catch (Throwable error) {
            Log.d(TAG, "hasPromotableCharacteristics unavailable");
        }
        Log.i(
                TAG,
                "live notification promotable=" + promotable
                        + " allowed=" + canPostPromotedNotifications(context)
        );
    }

    /** True on OPPO, OnePlus and realme devices running ColorOS / OxygenOS / realme UI. */
    static boolean isOplusFamily() {
        String fingerprint = deviceFingerprint();
        return fingerprint.contains("oppo")
                || fingerprint.contains("oneplus")
                || fingerprint.contains("realme")
                || fingerprint.contains("oplus");
    }

    static boolean isXiaomiFamily() {
        String fingerprint = deviceFingerprint();
        return fingerprint.contains("xiaomi")
                || fingerprint.contains("redmi")
                || fingerprint.contains("poco");
    }

    static boolean isHuaweiFamily() {
        String fingerprint = deviceFingerprint();
        return fingerprint.contains("huawei") || fingerprint.contains("honor");
    }

    static boolean isVivoFamily() {
        String fingerprint = deviceFingerprint();
        return fingerprint.contains("vivo") || fingerprint.contains("iqoo");
    }

    static boolean isAndroid16OrNewer() {
        return Build.VERSION.SDK_INT >= API_ANDROID_16;
    }

    /** True when this device has any live/capsule surface worth guiding the user to. */
    static boolean hasVendorLiveSurface() {
        return isOplusFamily() || isXiaomiFamily() || isHuaweiFamily() || isAndroid16OrNewer();
    }

    /** Human readable name of the live surface this device would use, for settings. */
    static String surfaceName(Context context) {
        if (context == null) return "";
        if (isOplusFamily()) return context.getString(R.string.live_surface_coloros);
        if (isXiaomiFamily()) return context.getString(R.string.live_surface_hyperos);
        if (isHuaweiFamily()) return context.getString(R.string.live_surface_harmony);
        if (isVivoFamily()) return context.getString(R.string.live_surface_originos);
        if (isAndroid16OrNewer()) return context.getString(R.string.live_surface_android16);
        return context.getString(R.string.live_surface_standard);
    }

    /**
     * One line telling the user what to expect on this device, so a missing capsule is
     * explained instead of looking like a bug.
     */
    static String surfaceDescription(Context context) {
        if (context == null) return "";
        if (isOplusFamily()) return context.getString(R.string.live_surface_coloros_hint);
        if (isAndroid16OrNewer()) return context.getString(R.string.live_surface_android16_hint);
        return context.getString(R.string.live_surface_standard_hint);
    }

    private static String normalizeShortText(String raw) {
        String value = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
        if (value.isEmpty()) return "";
        return value.length() <= MAX_SHORT_TEXT_LENGTH
                ? value
                : value.substring(0, MAX_SHORT_TEXT_LENGTH);
    }

    private static String deviceFingerprint() {
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String brand = Build.BRAND == null ? "" : Build.BRAND;
        return (manufacturer + " " + brand).toLowerCase(Locale.ROOT);
    }

    private static void addExtra(Notification.Builder builder, String key, Object value) {
        try {
            Bundle extras = new Bundle();
            if (value instanceof Boolean) {
                extras.putBoolean(key, (Boolean) value);
            } else {
                extras.putString(key, String.valueOf(value));
            }
            builder.addExtras(extras);
        } catch (Throwable error) {
            Log.d(TAG, "notification extra rejected: " + key);
        }
    }

    private static Object newInstance(String className) {
        try {
            Class<?> type = Class.forName(className);
            return type.getDeclaredConstructor().newInstance();
        } catch (Throwable error) {
            Log.d(TAG, "vendor class unavailable: " + className);
            return null;
        }
    }

    /**
     * Calls {@code target.methodName(arg)} by name. Every mismatch (missing method,
     * changed signature, disabled flag) is logged and ignored.
     *
     * @return true when a matching method was found and invoked successfully.
     */
    private static boolean invokeOneArg(Object target, String methodName, Object argument) {
        if (target == null || argument == null) return false;
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1) continue;
            if (!isArgumentCompatible(parameters[0], argument)) continue;
            try {
                method.setAccessible(true);
                method.invoke(target, argument);
                return true;
            } catch (Throwable error) {
                Log.d(TAG, "reflective call failed: " + methodName);
                return false;
            }
        }
        Log.d(TAG, "reflective method unavailable: " + methodName);
        return false;
    }

    private static boolean isArgumentCompatible(Class<?> parameterType, Object argument) {
        if (parameterType.isInstance(argument)) return true;
        if (!parameterType.isPrimitive()) return false;
        if (parameterType == boolean.class) return argument instanceof Boolean;
        if (parameterType == int.class) return argument instanceof Integer;
        if (parameterType == long.class) return argument instanceof Long;
        if (parameterType == float.class) return argument instanceof Float;
        if (parameterType == double.class) return argument instanceof Double;
        return false;
    }
}
