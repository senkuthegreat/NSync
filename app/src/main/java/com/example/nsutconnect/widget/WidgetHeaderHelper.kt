package com.example.nsutconnect.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.nsutconnect.R

object WidgetHeaderHelper {

    /**
     * Renders a high-resolution, anti-aliased bitmap for the widget header containing:
     * - "N" in white using the official Unkempt bold cursive font
     * - "Sync" in #6366F1 using the official Unkempt bold cursive font
     * - Section name ("Attendance" / "Schedule") in #6366F1 using bold sans-serif
     *
     * This bypasses Android RemoteViews' limitation of not loading custom font assets
     * from APK resources across the AppWidgetHost / launcher process boundary.
     */
    fun createHeaderBitmap(context: Context, sectionName: String): Bitmap {
        val density = context.resources.displayMetrics.density
        val heightPx = (32 * density).toInt()
        val widthPx = (260 * density).toInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val unkempt = ResourcesCompat.getFont(context, R.font.unkempt_bold) ?: Typeface.DEFAULT_BOLD
        val sansMedium = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            Typeface.create(Typeface.SANS_SERIF, 500, false)
        } else {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        val paintN = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = unkempt
            color = Color.WHITE
            textSize = 20f * density
        }

        val paintSync = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = unkempt
            color = 0xFF6366F1.toInt()
            textSize = 20f * density
        }

        val paintSection = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansMedium
            color = 0xFF6366F1.toInt()
            textSize = 15f * density
        }

        val baseline = 22f * density
        var curX = 0f

        canvas.drawText("N", curX, baseline, paintN)
        curX += paintN.measureText("N")

        canvas.drawText("Sync", curX, baseline, paintSync)
        curX += paintSync.measureText("Sync") + (6f * density)

        canvas.drawText(sectionName, curX, baseline, paintSection)

        return bitmap
    }
}
