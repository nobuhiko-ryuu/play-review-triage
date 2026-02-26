package app.playreviewtriage.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.playreviewtriage.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "daily_review"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "NotificationHelper"
    }

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "レビュー通知",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "重要なレビューがある場合に通知します"
        }
        notificationManager().createNotificationChannel(channel)
    }

    fun notifyHighReviews(count: Int) {
        // 通知が無効なら何もしない（Worker は正常完了させる）
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        // Android 13+（API 33+）は POST_NOTIFICATIONS 権限が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("重要レビューがあります")
            .setContentText("今日の重要レビュー：${count}件")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager().notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "通知の送信に失敗しました（権限なし）", e)
        }
    }

    private fun notificationManager() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
