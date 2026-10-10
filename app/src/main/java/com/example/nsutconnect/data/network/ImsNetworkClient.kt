package com.example.nsutconnect.data.network

import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.data.model.WeeklyTimetable
import com.example.nsutconnect.data.parser.ImsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

class ImsNetworkClient {

    companion object {
        const val BASE_URL = "https://www.imsnsit.org/imsnsit/"
        const val TIMETABLE_URL = "https://www.imsnsit.org/imsnsit/plum_url.php?41uZnOKcTatoQuZ1nS5qVGIuiLOuCZGGw43gNaDJuC6JU+zlhWK6ya/tcFLywtouIo7aWVZdCbDde4Onb0dyebiOoF5CwOCF9cWNylj6wkXd0RX8bVnVGaPKlMtStKmyuAZXCzEdKq+zBvagqtna+Vjw3K+2YkEhpEDLFH9RwuVsL162Qyo3TRed5amA2UTcldG13/T8xEKGB82zR7JxVA=="
        const val ATTENDANCE_URL = "https://www.imsnsit.org/imsnsit/plum_url.php?P59Otr7+3xJIlCBBABoVb0GIgAXiKbA1LIAMUeXV5ZvLND/AfclikCFEYYIO4oq3BkrlNPPPoLsLVLt2ihg6VdTT8AXD3bdKB07WmGbjTQ4PM9NWTedZH8r2ohs9zsBsbFmAs01PXjfJn07m4ARSsaaeuZxTy4F4EJp+SoAk4dHNGvsD3yrO6W3oQUld+8626tyicw9vPYtiH/KgrqEbUsWbLNgbppb9cgWn/IrjvV15vuFRdLZIHnwX5Qndspns"
        const val RESULT_URL = "https://www.imsnsit.org/imsnsit/plum_url.php?PUjRr5pFGNoE8gXcu3QXFifq7Q1T5bVMcKM+onSV1cc31UinqmKsKcAG92gk1aPHMePz0+SiG6Mx5L/zL7qXjSuTrfFf6DshrNGl9LuttJrEBcFcsiBunE0bcJI9KPSS7/jirI/6wxXoIqLxXeC2hPcYgGYu/LGXG7JzhP3y9Iw="
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.119 Safari/537.36"
    }

    suspend fun fetchLiveTimetable(cookies: String, rollNumber: String): WeeklyTimetable? = withContext(Dispatchers.IO) {
        try {
            val response = Jsoup.connect(TIMETABLE_URL)
                .userAgent(USER_AGENT)
                .header("Referer", "https://www.imsnsit.org/imsnsit/student_login.php")
                .apply {
                    if (cookies.isNotBlank()) header("Cookie", cookies)
                }
                .timeout(8000)
                .get()

            val html = response.html()
            if (html.contains("Time Table") || html.contains("T1")) {
                return@withContext ImsParser.parseTimetableHtml(html, rollNumber)
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchLiveAttendance(cookies: String, rollNumber: String): AttendanceSummary? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(ATTENDANCE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", USER_AGENT)
                if (cookies.isNotBlank()) setRequestProperty("Cookie", cookies)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Referer", ATTENDANCE_URL)
            }

            val postData = "year=2026-27&sem=3&submit=Submit&recentitycode=$rollNumber&dept=ELECTRONICS+AND+COMMUNICATION+ENGINEERING&degree=B.Tech."
            conn.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
            }

            val html = conn.inputStream.bufferedReader().use { it.readText() }
            if (html.contains("ABSENT") || html.contains("Total Classes") || html.contains("Overall")) {
                return@withContext ImsParser.parseAttendanceHtml(html, rollNumber)
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
