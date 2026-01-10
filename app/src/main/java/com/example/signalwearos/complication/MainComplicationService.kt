package com.example.signalwearos.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.signalwearos.R
import com.example.signalwearos.presentation.MainActivity

/**
 * Complication data source that returns a shortcut to the Signal app.
 * It shows the Signal icon and "Signal" text.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("Signal", "Open Signal")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) {
            return null
        }
        // In a real app, you could fetch unread message count here
        return createComplicationData("Signal", "Open Signal")
    }

    private fun createComplicationData(text: String, contentDescription: String): ShortTextComplicationData {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_message_24)
                ).build()
            )
            .setTapAction(pendingIntent)
            .build()
    }
}
