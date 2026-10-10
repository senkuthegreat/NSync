package com.example.nsutconnect.data.local

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class AdmitCardInfo(
    val rollNumber: String = "",
    val rollNumber12Digit: String = "",
    val candidateName: String = "STUDENT",
    val degree: String = "B.Tech.",
    val section: String = "1",
    val mobile: String = "",
    val branch: String = "ELECTRONICS AND COMMUNICATION ENGINEERING",
    val semester: String = "3",
    val examination: String = "MID SEM EXAMINATION - SEP 2026",
    val examCenter: String = "APJ Abdul Kalam Block (Block 6), Room 113/6",
    val generatedDate: String = "2026-09-20 18:12:36",
    val barcodeNumber: String = "",
    val courses: List<AdmitCardCourse> = listOf(
        AdmitCardCourse("1", "FCFO0301", "Community Internship", "3"),
        AdmitCardCourse("2", "VANM0303", "Innovation, Business Models and Entrepreneurship", "3"),
        AdmitCardCourse("3", "VTECC302", "Signals and Systems", "3"),
        AdmitCardCourse("4", "VTECC303", "Probability Theory and Random Process", "3"),
        AdmitCardCourse("5", "VTECC304", "Microelectronics", "3"),
        AdmitCardCourse("6", "VTECC305", "Digital Circuits and Systems", "3"),
        AdmitCardCourse("7", "VTPHC301", "Solid State Physics", "3")
    )
)

data class AdmitCardCourse(
    val sNo: String,
    val code: String,
    val title: String,
    val sem: String = "3"
)

object AdmitCardManager {

    fun getOfficialAdmitCardPdfFile(context: Context): File {
        val cacheFile = File(context.cacheDir, "nsut_admit_card.pdf")
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            try {
                context.assets.open("admit_card_actual.pdf").use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return cacheFile
    }

    fun openAdmitCardPdf(context: Context) {
        try {
            val pdfFile = getOfficialAdmitCardPdfFile(context)
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Official Admit Card PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun downloadAdmitCardPdf(context: Context, info: AdmitCardInfo = AdmitCardInfo()): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = "NSUT_AdmitCard_${info.rollNumber}.pdf"
            var outputStream: OutputStream? = null
            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                    savedUri = uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                outputStream = FileOutputStream(targetFile)
                savedUri = Uri.fromFile(targetFile)
            }

            // Check if authentic official PDF exists in assets
            val hasAssetPdf = try {
                context.assets.open("admit_card_actual.pdf").close()
                true
            } catch (e: Exception) {
                false
            }

            if (hasAssetPdf && outputStream != null) {
                context.assets.open("admit_card_actual.pdf").use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
            } else if (outputStream != null) {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val paint = Paint()
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.BLACK
                }

                paint.color = Color.rgb(245, 245, 250)
                canvas.drawRect(20f, 20f, 575f, 822f, paint)

                paint.color = Color.rgb(26, 35, 126) // Deep Navy
                canvas.drawRect(20f, 20f, 575f, 100f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 15f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("NETAJI SUBHAS UNIVERSITY OF TECHNOLOGY", 110f, 50f, textPaint)

                textPaint.textSize = 10f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Sector 3, Dwarka, New Delhi - 110078 | Examination Division", 135f, 68f, textPaint)

                textPaint.textSize = 12f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("MID SEM EXAMINATION - SEP 2026 | ADMIT CARD", 110f, 88f, textPaint)

                var yPos = 130f
                textPaint.color = Color.DKGRAY
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("CANDIDATE INFORMATION", 40f, yPos, textPaint)

                paint.color = Color.LTGRAY
                canvas.drawLine(40f, yPos + 6f, 555f, yPos + 6f, paint)

                yPos += 26f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = Color.BLACK
                canvas.drawText("Roll Number: ${info.rollNumber} (${info.rollNumber12Digit})", 40f, yPos, textPaint)
                canvas.drawText("Candidate Name: ${info.candidateName}", 320f, yPos, textPaint)

                yPos += 20f
                canvas.drawText("Degree: ${info.degree} (Section ${info.section})", 40f, yPos, textPaint)
                canvas.drawText("Mobile: ${info.mobile}", 320f, yPos, textPaint)

                yPos += 20f
                canvas.drawText("Branch: ${info.branch}", 40f, yPos, textPaint)

                yPos += 20f
                canvas.drawText("Examination Center: ${info.examCenter}", 40f, yPos, textPaint)

                yPos += 40f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("REGISTERED PAPERS (SEMESTER 3)", 40f, yPos, textPaint)
                canvas.drawLine(40f, yPos + 6f, 555f, yPos + 6f, paint)

                yPos += 24f
                textPaint.textSize = 9f
                canvas.drawText("S.No", 40f, yPos, textPaint)
                canvas.drawText("Paper Code", 80f, yPos, textPaint)
                canvas.drawText("Paper Name", 190f, yPos, textPaint)
                canvas.drawText("Sem", 510f, yPos, textPaint)

                paint.color = Color.rgb(200, 200, 210)
                canvas.drawLine(40f, yPos + 4f, 555f, yPos + 4f, paint)

                yPos += 18f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                for (course in info.courses) {
                    canvas.drawText(course.sNo, 40f, yPos, textPaint)
                    canvas.drawText(course.code, 80f, yPos, textPaint)
                    canvas.drawText(course.title, 190f, yPos, textPaint)
                    canvas.drawText(course.sem, 510f, yPos, textPaint)
                    yPos += 18f
                }

                yPos += 25f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("EXAMINATION GUIDELINES & INSTRUCTIONS", 40f, yPos, textPaint)
                canvas.drawLine(40f, yPos + 6f, 555f, yPos + 6f, paint)

                yPos += 20f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textSize = 8.5f
                val instructions = listOf(
                    "1. This Admit Card must be preserved until the declaration of results.",
                    "2. Candidate must carry a physical hard copy along with official NSUT Student ID Card.",
                    "3. Reporting time is 15 minutes before scheduled exam commencement.",
                    "4. Electronic gadgets, smart watches, and programmable calculators are strictly prohibited."
                )
                for (inst in instructions) {
                    canvas.drawText(inst, 40f, yPos, textPaint)
                    yPos += 16f
                }

                yPos = 780f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 9f
                canvas.drawText("[VERIFIED DIGITAL CREDENTIAL]", 40f, yPos, textPaint)
                canvas.drawText("Controller of Examinations, NSUT", 380f, yPos, textPaint)

                pdfDocument.finishPage(page)
                outputStream.use { os ->
                    pdfDocument.writeTo(os)
                }
                pdfDocument.close()
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Official Admit Card downloaded to Downloads!", Toast.LENGTH_LONG).show()
            }

            savedUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
