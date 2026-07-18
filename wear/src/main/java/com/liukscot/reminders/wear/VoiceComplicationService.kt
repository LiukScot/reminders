package com.liukscot.reminders.wear

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

// One tap from the watch face to the microphone. The content never changes — this is an action,
// not a readout — so the manifest sets UPDATE_PERIOD_SECONDS to 0 and the system never polls it.
class VoiceComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = complicationData(type)

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener,
    ) {
        listener.onComplicationData(complicationData(request.complicationType))
    }

    private fun complicationData(type: ComplicationType): ComplicationData? {
        val description = PlainComplicationText.Builder(
            getString(R.string.voice_complication_description),
        ).build()
        val icon = MonochromaticImage.Builder(
            Icon.createWithResource(this, R.drawable.ic_mic),
        ).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    getString(R.string.voice_complication_label),
                ).build(),
                contentDescription = description,
            ).setMonochromaticImage(icon).setTapAction(voiceIntent()).build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = icon,
                contentDescription = description,
            ).setTapAction(voiceIntent()).build()

            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                smallImage = SmallImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_mic),
                    SmallImageType.ICON,
                ).build(),
                contentDescription = description,
            ).setTapAction(voiceIntent()).build()

            // The manifest only advertises the three above; anything else means the watch face
            // asked for a type this data source never claimed to serve.
            else -> null
        }
    }

    private fun voiceIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
