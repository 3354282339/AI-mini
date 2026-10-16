package com.coimgrain.codexminiapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import org.json.JSONObject;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class TaskNotificationStyle {
    private static final int MAX_SUMMARY_LENGTH = 520;
    private static final String TASK_NOTIFICATION_GROUP = "app.aimini.TASK_NOTIFICATIONS";
    private static final Pattern MARKDOWN_LINK = Pattern.compile("!?\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("(?m)^#{1,6}\\s*");
    private static final Pattern MARKDOWN_MARKER = Pattern.compile("[*_~`]+");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String TERMINAL_LEDGER_PREFIX =
            "task_terminal_notification_v1:";
    private static final long UNKNOWN_RUN_DEDUP_WINDOW_MS = 6L * 60L * 60L * 1000L;
    private static final long STARTED_AT_CLOCK_TOLERANCE_MS = 2000L;
    /** 设备类型 → 通知大图缓存。 */
    private static final Map<String, Bitmap> deviceBadgeCache = new HashMap<>();

    private TaskNotificationStyle() {
    }

    static Notification buildTerminalNotification(
            Context context,
            int notificationId,
            String channelId,
            String threadId,
            String threadName,
            boolean error,
            String rawSummary,
            long durationMs,
            String deviceUrl,
            String deviceKind
    ) {
        String safeThreadName = cleanThreadName(context, threadName);
        String title = context.getString(
                error ? R.string.task_error_title : R.string.task_complete_title,
                safeThreadName
        );
        String summary = cleanSummary(rawSummary);
        if (summary.isEmpty()) {
            summary = context.getString(
                    error
                            ? R.string.task_error_summary_fallback
                            : R.string.task_complete_summary_fallback
            );
        }
        String duration = formatDuration(durationMs);
        String body = duration.isEmpty()
                ? summary
                : summary + "\n\n" + context.getString(R.string.task_duration, duration);

        Intent launchIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_THREAD_ID, safeThreadId(threadId))
                // 多设备场景：点通知必须回到"这条通知所属的那台设备"，而不是当前设备。
                .putExtra(
                        MainActivity.EXTRA_OPEN_DEVICE_URL,
                        deviceUrl == null ? "" : deviceUrl.trim()
                );
        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent markReadIntent = new Intent(context, TaskNotificationActionReceiver.class)
                .setAction(TaskNotificationActionReceiver.ACTION_MARK_READ)
                .putExtra(TaskNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        PendingIntent markReadPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                markReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(context, channelId);
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(body);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(deviceBadgeIcon(context, deviceKind))
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(bigTextStyle)
                .setContentIntent(launchPendingIntent)
                .setDeleteIntent(markReadPendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setShowWhen(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(Color.rgb(101, 86, 217))
                .setGroup(TASK_NOTIFICATION_GROUP)
                .addAction(new Notification.Action.Builder(
                        null,
                        context.getString(R.string.task_view_reply),
                        launchPendingIntent
                ).build())
                .addAction(new Notification.Action.Builder(
                        null,
                        context.getString(R.string.task_mark_read),
                        markReadPendingIntent
                ).build());
        return builder.build();
    }

    /**
     * Builds the ongoing "live" notification shared by the foreground service and
     * the per-conversation running state.
     *
     * <p>Only platform-level live signals are used here on purpose: the ongoing
     * flag, a chronometer anchored at the task start, and (while a task runs) an
     * indeterminate progress bar. Those are exactly the signals Android 16
     * promoted/progress notifications and vendor "capsule / 实况窗 / 灵动岛" surfaces
     * promote, while a device that supports none of them still renders a normal
     * ongoing notification.</p>
     */
    static Notification buildLiveNotification(
            Context context,
            int notificationId,
            String channelId,
            String title,
            String content,
            String threadId,
            long startedAt,
            boolean running
    ) {
        Intent launchIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_THREAD_ID, safeThreadId(threadId));
        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        boolean chronometer = running && startedAt > 0L;
        Notification.Builder builder = new Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setContentIntent(launchPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(chronometer)
                .setWhen(chronometer ? startedAt : System.currentTimeMillis())
                .setUsesChronometer(chronometer)
                .setCategory(running
                        ? Notification.CATEGORY_PROGRESS
                        : Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (running) {
            builder.setProgress(0, 0, true);
        }
        String shortCriticalText = running
                ? context.getString(R.string.live_short_text_running)
                : "";
        Notification notification = LiveNotificationSupport
                .decorate(context, builder, running, shortCriticalText)
                .build();
        if (running) {
            LiveNotificationSupport.logPromotionState(context, notification);
        }
        return notification;
    }

    /**
     * Atomically claims one terminal alert for a task run.
     *
     * <p>The foreground service and MainActivity receive task state through
     * independent paths. Android can resume the page after the service has already
     * posted the completion alert, causing the page to replay the same terminal
     * state. Persisting the claim makes that replay silent even after the activity
     * has been recreated or the original notification has been tapped and removed.</p>
     */
    static synchronized boolean claimTerminalNotification(
            Context context,
            String threadId,
            String threadName,
            boolean error,
            long startedAt
    ) {
        if (context == null) return false;
        String identity = terminalIdentity(threadId, threadName);
        SharedPreferences preferences = context.getSharedPreferences(
                MainActivity.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        String preferenceKey = TERMINAL_LEDGER_PREFIX + identity;
        long now = System.currentTimeMillis();
        long previousPostedAt = 0L;
        long previousStartedAt = 0L;
        try {
            JSONObject previous = new JSONObject(
                    preferences.getString(preferenceKey, "{}")
            );
            previousPostedAt = previous.optLong("postedAt", 0L);
            previousStartedAt = previous.optLong("startedAt", 0L);
        } catch (Exception ignored) {
        }

        if (previousPostedAt > 0L) {
            if (startedAt > 0L) {
                // A new run cannot have started before the previous terminal alert.
                // A small tolerance absorbs timestamp rounding between WebUI and
                // Android without suppressing a genuinely newer task.
                if (startedAt <= previousPostedAt + STARTED_AT_CLOCK_TOLERANCE_MS) {
                    return false;
                }
            } else if (now - previousPostedAt <= UNKNOWN_RUN_DEDUP_WINDOW_MS) {
                // After the service removes the completed task from preferences,
                // the resumed page no longer has a native startedAt value. Treat a
                // near-term replay for the same conversation as the same run.
                return false;
            }
        }

        try {
            JSONObject claimed = new JSONObject();
            claimed.put("identity", identity);
            claimed.put("startedAt", startedAt > 0L ? startedAt : previousStartedAt);
            claimed.put("postedAt", now);
            claimed.put("error", error);
            preferences.edit().putString(preferenceKey, claimed.toString()).commit();
        } catch (Exception ignored) {
            // Failing to persist must not suppress a real terminal notification.
        }
        return true;
    }

    private static String terminalIdentity(String threadId, String threadName) {
        String id = threadId == null ? "" : threadId.trim();
        if (!id.isEmpty() && !"current".equals(id) && !id.startsWith("pending-")) {
            return "thread:" + id;
        }
        String name = threadName == null
                ? ""
                : threadName.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        if (!name.isEmpty()) return "name:" + name;
        return "thread:current";
    }

    static String summaryFromStatus(JSONObject status, boolean error) {
        if (status == null) return "";
        if (error) {
            String failure = jsonText(status.opt("error"));
            if (!failure.isEmpty()) return failure;
        }
        String summary = status.optString("final", "").trim();
        if (summary.isEmpty()) summary = status.optString("preview", "").trim();
        if (summary.isEmpty()) summary = status.optString("summary", "").trim();
        if (summary.isEmpty() && error) summary = status.optString("message", "").trim();
        return summary;
    }

    static long durationMsFromStatus(JSONObject status, long startedAt) {
        if (status == null) return 0L;
        long durationMs = Math.max(0L, status.optLong("durationMs", 0L));
        if (durationMs > 0L) return durationMs;
        long start = timestampMillis(status.opt("startedAt"));
        if (start <= 0L) start = startedAt;
        long end = timestampMillis(status.opt("completedAt"));
        if (end <= 0L) end = timestampMillis(status.opt("updatedAt"));
        if (start > 0L && end >= start) return end - start;
        return 0L;
    }

    static String cleanSummary(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return "";
        value = MARKDOWN_LINK.matcher(value).replaceAll("$1");
        value = MARKDOWN_HEADING.matcher(value).replaceAll("");
        value = MARKDOWN_MARKER.matcher(value).replaceAll("");
        value = HTML_TAG.matcher(value).replaceAll(" ");
        value = WHITESPACE.matcher(value).replaceAll(" ").trim();
        if (value.length() <= MAX_SUMMARY_LENGTH) return value;
        return value.substring(0, MAX_SUMMARY_LENGTH - 1).trim() + "…";
    }

    static String formatDuration(long durationMs) {
        if (durationMs <= 0L) return "";
        long totalSeconds = Math.max(1L, Math.round(durationMs / 1000d));
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return minutes > 0L
                    ? String.format(Locale.CHINA, "%d小时%d分", hours, minutes)
                    : String.format(Locale.CHINA, "%d小时", hours);
        }
        if (minutes > 0L) {
            return seconds > 0L
                    ? String.format(Locale.CHINA, "%d分%d秒", minutes, seconds)
                    : String.format(Locale.CHINA, "%d分", minutes);
        }
        return String.format(Locale.CHINA, "%d秒", seconds);
    }

    private static String cleanThreadName(Context context, String threadName) {
        String value = threadName == null ? "" : threadName.replaceAll("\\s+", " ").trim();
        while (value.length() > 0
                && value.length() % 2 == 0
                && value.substring(0, value.length() / 2)
                .equals(value.substring(value.length() / 2))) {
            value = value.substring(0, value.length() / 2).trim();
        }
        if (value.isEmpty() || "选择线程".equals(value)) {
            return context.getString(R.string.task_complete_fallback);
        }
        return value.length() <= 80 ? value : value.substring(0, 79).trim() + "…";
    }

    private static String safeThreadId(String threadId) {
        return threadId == null ? "" : threadId.trim();
    }

    private static String jsonText(Object value) {
        if (value == null || value == JSONObject.NULL) return "";
        if (value instanceof Boolean && !((Boolean) value)) return "";
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return "";
        }
        return text;
    }

    static long timestampMillis(Object value) {
        if (value == null || value == JSONObject.NULL) return 0L;
        if (value instanceof Number) {
            long number = ((Number) value).longValue();
            return number > 0L && number < 100000000000L ? number * 1000L : number;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return 0L;
        try {
            long number = Long.parseLong(text);
            return number > 0L && number < 100000000000L ? number * 1000L : number;
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(text).toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    /**
     * 按设备类型生成的通知大图（类型流光底 + 缩写），相当于「好友头像」，
     * 让多设备下的完成通知一眼看出是哪台设备发来的。
     */
    private static synchronized Bitmap deviceBadgeIcon(Context context, String deviceKind) {
        String kind = deviceKind == null ? "" : deviceKind.trim();
        Bitmap cached = deviceBadgeCache.get(kind);
        if (cached != null) return cached;
        int size = Math.max(
                64,
                Math.round(56f * context.getResources().getDisplayMetrics().density)
        );
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int[] gradient = MainActivity.deviceKindGradient(kind);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(
                0f,
                0f,
                size,
                size,
                gradient[0],
                gradient[2],
                Shader.TileMode.CLAMP
        ));
        float radius = size * 0.3f;
        canvas.drawRoundRect(new RectF(0f, 0f, size, size), radius, radius, paint);
        paint.setShader(null);
        paint.setColor(Color.rgb(12, 20, 34));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size * 0.32f);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = size / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(MainActivity.deviceKindBadge(kind), size / 2f, baseline, paint);
        deviceBadgeCache.put(kind, bitmap);
        return bitmap;
    }
}
